package com.cloudbees.jenkins;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import hudson.Extension;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Project;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;
import jenkins.model.Jenkins.MasterComputer;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.jelly.XMLOutput;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import org.jenkinsci.plugins.github.internal.GHPluginConfigException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Triggers a build when we receive a GitHub post-commit webhook.
 *
 * @author Kohsuke Kawaguchi
 */
public class GitHubPushTrigger extends Trigger<AbstractProject<?, ?>> implements GitHubTrigger {
    @DataBoundConstructor
    public GitHubPushTrigger() {
    }

    /**
     * Called when a POST is made.
     */
    @Deprecated
    public void onPost() {
        onPost("");
    }

    /**
     * Called when a POST is made.
     */
    public void onPost(String triggeredByUser) {
        final String pushBy = triggeredByUser;
        getDescriptor().queue.execute(new Runnable() {
            private boolean runPolling() {
                try {
                    StreamTaskListener listener = new StreamTaskListener(getLogFile());

                    try {
                        PrintStream logger = listener.getLogger();
                        long start = System.currentTimeMillis();
                        logger.println("Started on " + DateFormat.getDateTimeInstance().format(new Date()));
                        boolean result = job.poll(listener).hasChanges();
                        logger.println("Done. Took " + Util.getTimeSpanString(System.currentTimeMillis() - start));
                        if (result) {
                            logger.println("Changes found");
                        } else {
                            logger.println("No changes");
                        }
                        return result;
                    } catch (Error e) {
                        e.printStackTrace(listener.error("Failed to record SCM polling"));
                        LOGGER.log(Level.SEVERE,"Failed to record SCM polling",e);
                        throw e;
                    } catch (RuntimeException e) {
                        e.printStackTrace(listener.error("Failed to record SCM polling"));
                        LOGGER.log(Level.SEVERE,"Failed to record SCM polling",e);
                        throw e;
                    } finally {
                        listener.close();
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE,"Failed to record SCM polling",e);
                }
                return false;
            }

            public void run() {
                if (runPolling()) {
                    String name = " #" + job.getNextBuildNumber();
                    GitHubPushCause cause;
                    try {
                        cause = new GitHubPushCause(getLogFile(), pushBy);
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Failed to parse the polling log",e);
                        cause = new GitHubPushCause(pushBy);
                    }
                    if (job.scheduleBuild(cause)) {
                        LOGGER.info("SCM changes detected in "+ job.getName()+". Triggering "+name);
                    } else {
                        LOGGER.info("SCM changes detected in "+ job.getName()+". Job is already in the queue");
                    }
                }
            }
        });
    }

    /**
     * Returns the file that records the last/current polling activity.
     */
    public File getLogFile() {
        return new File(job.getRootDir(), "github-polling.log");
    }

    /**
     * @deprecated Use {@link GitHubRepositoryNameContributor#parseAssociatedNames(AbstractProject)}
     */
    @Deprecated
    public Set<GitHubRepositoryName> getGitHubRepositories() {
        return Collections.emptySet();
    }

    @Override
    public void start(AbstractProject<?, ?> project, boolean newInstance) {
        super.start(project, newInstance);
        if (newInstance && getDescriptor().isManageHook()) {
            registerHooks();
        }
    }

    /**
     * Tries to register hook for current associated job.
     * Do this lazily to avoid blocking the UI thread.
     * Useful for using from groovy scripts.
     *
     * @since 1.11.2
     */
    public void registerHooks() {
        GitHubWebHook.get().registerHookFor(job);
    }


    @Override
    public void stop() {
        if (job == null) {
            return;
        }

        if (getDescriptor().isManageHook()) {
            Cleaner cleaner = Cleaner.get();
            if (cleaner != null) {
                cleaner.onStop(job);
            }
        }
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        if (job == null) {
            return Collections.emptyList();
        }

        return Collections.singleton(new GitHubWebHookPollingAction());
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Action object for {@link Project}. Used to display the polling log.
     */
    public final class GitHubWebHookPollingAction implements Action {
        public AbstractProject<?, ?> getOwner() {
            return job;
        }

        public String getIconFileName() {
            return "clipboard.png";
        }

        public String getDisplayName() {
            return "GitHub Hook Log";
        }

        public String getUrlName() {
            return "GitHubPollLog";
        }

        public String getLog() throws IOException {
            return Util.loadFile(getLogFile());
        }

        /**
         * Writes the annotated log to the given output.
         *
         * @since 1.350
         */
        public void writeLogTo(XMLOutput out) throws IOException {
            new AnnotatedLargeText<GitHubWebHookPollingAction>(getLogFile(), Charsets.UTF_8, true, this).writeHtmlTo(0, out.asWriter());
        }
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {
        private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class.getName());
        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(MasterComputer.threadPoolForRemoting);

        private boolean manageHook;
        private String hookUrl;
        private volatile List<Credential> credentials = new ArrayList<Credential>();

        @Inject
        private transient InstanceIdentity identity;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof AbstractProject;
        }

        @Override
        public String getDisplayName() {
            return "Build when a change is pushed to GitHub";
        }

        /**
         * True if Jenkins should auto-manage hooks.
         */
        public boolean isManageHook() {
            return manageHook;
        }

        public void setManageHook(boolean v) {
            manageHook = v;
            save();
        }

        /**
         * Returns the URL that GitHub should post.
         */
        public URL getHookUrl() throws GHPluginConfigException {
            try {
                return hookUrl != null
                        ? new URL(hookUrl)
                        : new URL(Jenkins.getInstance().getRootUrl() + GitHubWebHook.get().getUrlName() + '/');
            } catch (MalformedURLException e) {
                throw new GHPluginConfigException(
                        "Mailformed GH hook url in global configuration (%s)", e.getMessage()
                );
            }
        }

        public boolean hasOverrideURL() {
            return hookUrl != null;
        }

        public List<Credential> getCredentials() {
            return credentials;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            JSONObject hookMode = json.getJSONObject("hookMode");
            manageHook = "auto".equals(hookMode.getString("value"));
            if (hookMode.optBoolean("hasHookUrl")) {
                hookUrl = hookMode.optString("hookUrl");
            } else {
                hookUrl = null;
            }
            credentials = req.bindJSONToList(Credential.class, hookMode.get("credentials"));
            save();
            return true;
        }

        public FormValidation doCheckHookUrl(@QueryParameter String value) {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(value).openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty(GitHubWebHook.URL_VALIDATION_HEADER, "true");
                con.connect();
                if (con.getResponseCode()!=200) {
                    return FormValidation.error("Got "+con.getResponseCode()+" from "+value);
                }
                String v = con.getHeaderField(GitHubWebHook.X_INSTANCE_IDENTITY);
                if (v == null) {
                    // people might be running clever apps that's not Jenkins, and that's OK
                    return FormValidation.warning("It doesn't look like " + value + " is talking to any Jenkins. Are you running your own app?");
                }
                RSAPublicKey key = identity.getPublic();
                String expected = new String(Base64.encodeBase64(key.getEncoded()));
                if (!expected.equals(v)) {
                    // if it responds but with a different ID, that's more likely wrong than correct
                    return FormValidation.error(value+" is connecting to different Jenkins instances");
                }

                return FormValidation.ok();
            } catch (IOException e) {
                return FormValidation.error(e,"Failed to test a connection to "+value);
            }

        }

        @SuppressWarnings("unused")
        public FormValidation doReRegister() {
            if (!manageHook) {
                return FormValidation.warning("Works only when Jenkins manages hooks");
            }

            List<AbstractProject> registered = GitHubWebHook.get().reRegisterAllHooks();

            LOGGER.log(Level.INFO, "Called registerHooks() for {0} jobs", registered.size());
            return FormValidation.ok("Called re-register hooks for %s jobs", registered.size());
        }

        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }

        public static boolean allowsHookUrlOverride() {
            return ALLOW_HOOKURL_OVERRIDE;
        }
    }

    /**
     * Set to false to prevent the user from overriding the hook URL.
     */
    public static boolean ALLOW_HOOKURL_OVERRIDE = !Boolean.getBoolean(GitHubPushTrigger.class.getName() + ".disableOverride");

    private static final Logger LOGGER = Logger.getLogger(GitHubPushTrigger.class.getName());
}

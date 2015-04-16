package com.cloudbees.jenkins;

import hudson.Extension;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Hudson.MasterComputer;
import hudson.model.Item;
import hudson.model.AbstractProject;
import hudson.model.Project;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
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

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.jelly.XMLOutput;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import org.kohsuke.github.GHException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.inject.Inject;

/**
 * Triggers a build when we receive a GitHub post-commit webhook.
 *
 * @author Kohsuke Kawaguchi
 */
public class GitHubPushTrigger extends Trigger<AbstractProject<?,?>> implements GitHubTrigger {
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
                        logger.println("Started on "+ DateFormat.getDateTimeInstance().format(new Date()));
                        boolean result = job.poll(listener).hasChanges();
                        logger.println("Done. Took "+ Util.getTimeSpanString(System.currentTimeMillis()-start));
                        if(result)
                            logger.println("Changes found");
                        else
                            logger.println("No changes");
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
                    String name = " #"+job.getNextBuildNumber();
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
        return new File(job.getRootDir(),"github-polling.log");
    }

    /**
     * @deprecated
     *      Use {@link GitHubRepositoryNameContributor#parseAssociatedNames(AbstractProject)}
     */
    public Set<GitHubRepositoryName> getGitHubRepositories() {
        return Collections.emptySet();
    }

    @Override
    public void start(AbstractProject<?,?> project, boolean newInstance) {
        super.start(project, newInstance);
        if (newInstance && getDescriptor().isManageHook()) {
            registerHooks();
        }
    }

    /**
     * Tries to register hook for current associated job.
     * Useful for using from groovy scripts.
     * @since 1.11.2
     */
    public void registerHooks() {
        // make sure we have hooks installed. do this lazily to avoid blocking the UI thread.
        final Collection<GitHubRepositoryName> names = GitHubRepositoryNameContributor.parseAssociatedNames(job);

        getDescriptor().queue.execute(new Runnable() {
            public void run() {
                LOGGER.log(Level.INFO, "Adding GitHub webhooks for {0}", names);

                for (GitHubRepositoryName name : names) {
                    for (GHRepository repo : name.resolve()) {
                        try {
                            if(createJenkinsHook(repo, getDescriptor().getHookUrl())) {
                                break;
                            }
                        } catch (Throwable e) {
                            LOGGER.log(Level.WARNING, "Failed to add GitHub webhook for "+name, e);
                        }
                    }
                }
            }
        });
    }

    private boolean createJenkinsHook(GHRepository repo, URL url) {
        try {
            if (getDescriptor().isUseWebHooks()) {
                // No need to pass events since the default event is PUSH
                repo.createWebHook(new URL(url.toExternalForm()));
            } else {
                repo.createHook("jenkins", Collections.singletonMap("jenkins_hook_url", url.toExternalForm()), null, true);
            }
            return true;
        } catch (IOException e) {
            throw new GHException("Failed to update jenkins hooks", e);
        }
    }

    @Override
    public void stop() {
        if (getDescriptor().isManageHook()) {
            Cleaner cleaner = Cleaner.get();
            if (cleaner != null) {
                cleaner.onStop(job);
            }
        }
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.singleton(new GitHubWebHookPollingAction());
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Action object for {@link Project}. Used to display the polling log.
     */
    public final class GitHubWebHookPollingAction implements Action {
        public AbstractProject<?,?> getOwner() {
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
         * @since 1.350
         */
        public void writeLogTo(XMLOutput out) throws IOException {
            new AnnotatedLargeText<GitHubWebHookPollingAction>(getLogFile(), Charset.defaultCharset(),true,this).writeHtmlTo(0,out.asWriter());
        }
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {
        private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class.getName());
        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(MasterComputer.threadPoolForRemoting);

        private boolean manageHook;
        private boolean useWebHooks;
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
         * True if the plugin should use webhooks as opposed to use the GitHub Jenkins Application to
         * register hooks.
         */
        public boolean isUseWebHooks() {
            return useWebHooks;
        }

        public void setUseWebHooks(boolean v) {
            useWebHooks = v;
            save();
        }

        /**
         * Returns the URL that GitHub should post.
         */
        public URL getHookUrl() throws MalformedURLException {
            return hookUrl!=null ? new URL(hookUrl) : new URL(Hudson.getInstance().getRootUrl()+GitHubWebHook.get().getUrlName()+'/');
        }

        public boolean hasOverrideURL() {
            return hookUrl!=null;
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
            useWebHooks = hookMode.optBoolean("useWebHooks");
            credentials = req.bindJSONToList(Credential.class,hookMode.get("credentials"));
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
                if (v==null) {
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

        public FormValidation doReRegister() {
            if (!manageHook) {
                return FormValidation.error("Works only when Jenkins manages hooks");
            }

            int triggered = 0;
            for (AbstractProject<?,?> job : getJenkinsInstance().getAllItems(AbstractProject.class)) {
                if (!job.isBuildable()) {
                    continue;
                }

                GitHubPushTrigger trigger = job.getTrigger(GitHubPushTrigger.class);
                if (trigger!=null) {
                    LOGGER.log(Level.FINE, "Calling registerHooks() for {0}", job.getFullName());
                    trigger.registerHooks();
                    triggered++;
                }
            }

            LOGGER.log(Level.INFO, "Called registerHooks() for {0} jobs", triggered);
            return FormValidation.ok("Called re-register hooks for " + triggered + " jobs");
        }

        public static final Jenkins getJenkinsInstance() throws IllegalStateException {
            Jenkins instance = Jenkins.getInstance();
            if (instance == null) {
                throw new IllegalStateException("Jenkins has not been started, or was already shut down");
            }
            return instance;
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
    public static boolean ALLOW_HOOKURL_OVERRIDE = !Boolean.getBoolean(GitHubPushTrigger.class.getName()+".disableOverride");

    private static final Logger LOGGER = Logger.getLogger(GitHubPushTrigger.class.getName());
}

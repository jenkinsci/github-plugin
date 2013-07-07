package com.cloudbees.jenkins;

import hudson.Extension;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.AbstractProject;
import hudson.model.Project;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.jelly.XMLOutput;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.multiplescms.MultiSCM;
import org.kohsuke.github.GHException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

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
     * Does this project read from a repository of the given user name and the
     * given repository name?
     */
    public Set<GitHubRepositoryName> getGitHubRepositories() {
        Set<GitHubRepositoryName> r = new HashSet<GitHubRepositoryName>();
        if (Hudson.getInstance().getPlugin("multiple-scms") != null
                && job.getScm() instanceof MultiSCM) {
            MultiSCM multiSCM = (MultiSCM) job.getScm();
            List<SCM> scmList = multiSCM.getConfiguredSCMs();
            for (SCM scm : scmList) {
                addRepositories(r, scm);
            }
        } else {
            addRepositories(r, job.getScm());
        }
        return r;
    }

    /**
     * @since 1.1
     */
    protected void addRepositories(Set<GitHubRepositoryName> r, SCM scm) {
        if (scm instanceof GitSCM) {
            GitSCM git = (GitSCM) scm;
            for (RemoteConfig rc : git.getRepositories()) {
                for (URIish uri : rc.getURIs()) {
                    String url = uri.toString();
                    GitHubRepositoryName repo = GitHubRepositoryName.create(url);
                    if (repo != null) {
                        r.add(repo);
                    }
                }
            }
        }
    }

    @Override
    public void start(AbstractProject<?,?> project, boolean newInstance) {
        super.start(project, newInstance);
        if (newInstance && getDescriptor().isManageHook()) {
            // make sure we have hooks installed. do this lazily to avoid blocking the UI thread.
            final Set<GitHubRepositoryName> names = getGitHubRepositories();

            getDescriptor().queue.execute(new Runnable() {
                public void run() {
                    OUTER:
                    for (GitHubRepositoryName name : names) {
                        for (GHRepository repo : name.resolve()) {
                            try {
                                if(createJenkinsHook(repo, getDescriptor().getHookUrl())) {
                                    LOGGER.info("Added GitHub webhook for "+name);
                                    continue OUTER;
                                }
                            } catch (Throwable e) {
                                LOGGER.log(Level.WARNING, "Failed to add GitHub webhook for "+name, e);
                            }
                        }
                    }
                }
            });
        }
    }

    private boolean createJenkinsHook(GHRepository repo, URL url) {
        try {
            repo.createHook("jenkins", Collections.singletonMap("jenkins_hook_url", url.toExternalForm()), null, true);
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
                cleaner.onStop(this);
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
        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Executors.newCachedThreadPool());

        private boolean manageHook;
        private String hookUrl;
        private volatile List<Credential> credentials = new ArrayList<Credential>();

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
            JSONObject o = hookMode.getJSONObject("hookUrl");
            if (o!=null && !o.isNullObject()) {
                hookUrl = o.getString("url");
            } else {
                hookUrl = null;
            }
            credentials = req.bindJSONToList(Credential.class,hookMode.get("credentials"));
            save();
            return true;
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

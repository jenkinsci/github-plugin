package com.cloudbees.jenkins;

import hudson.Extension;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Project;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;
import net.sf.json.JSONObject;
import org.apache.commons.jelly.XMLOutput;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.spearce.jgit.transport.RemoteConfig;
import org.spearce.jgit.transport.URIish;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Triggers a build when we receive a GitHub post-commit webhook.
 *
 * @author Kohsuke Kawaguchi
 */
public class GitHubPushTrigger extends Trigger<AbstractProject> implements Runnable {
    @DataBoundConstructor
    public GitHubPushTrigger() {
    }

    /**
     * Called when a POST is made.
     */
    public void onPost() {
        getDescriptor().queue.execute(this);
    }

    /**
     * Returns the file that records the last/current polling activity.
     */
    public File getLogFile() {
        return new File(job.getRootDir(),"github-polling.log");
    }

    public void run() {
        try {
            StreamTaskListener listener = new StreamTaskListener(getLogFile());

            try {
                PrintStream logger = listener.getLogger();
                long start = System.currentTimeMillis();
                logger.println("Started on "+ DateFormat.getDateTimeInstance().format(new Date()));
                boolean result = job.poll(listener).hasChanges();
                logger.println("Done. Took "+ Util.getTimeSpanString(System.currentTimeMillis()-start));
                if(result) {
                    logger.println("Changes found");
                    job.scheduleBuild(new GitHubPushCause());
                } else {
                    logger.println("No changes");
                }
            } finally {
                listener.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,"Failed to record SCM polling",e);
        }
    }

    /**
     * Does this project read from a repository of the given user name and the given repository name?
     */
    public Set<GitHubRepositoryName> getGitHubRepositories() {
        Set<GitHubRepositoryName> r = new HashSet<GitHubRepositoryName>();
        SCM scm = job.getScm();
        if (scm instanceof GitSCM) {
            GitSCM git = (GitSCM) scm;
            for (RemoteConfig rc : git.getRepositories()) {
                for (URIish uri : rc.getURIs()) {
                    String url = uri.toString();
                    for (Pattern p : URL_PATTERNS) {
                        Matcher m = p.matcher(url);
                        if (m.matches())
                            r.add(new GitHubRepositoryName(m.group(1),m.group(2)));
                    }
                }
            }
        }
        return r;
    }

    @Override
    public void start(AbstractProject project, boolean newInstance) {
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
                                if (repo.getPostCommitHooks().add(getDescriptor().getHookUrl())) {
                                    LOGGER.info("Added GitHub webhook for "+name);
                                    continue OUTER;
                                }
                            } catch (Throwable e) {
                                LOGGER.log(Level.WARNING, "Failed to add GitHub webhook for "+name,e);
                            }
                        }
                    }
                }
            });
        }
    }

    @Override
    public void stop() {
        if (getDescriptor().isManageHook())
            Cleaner.get().onStop(this);
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
            return "clipboard.gif";
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
        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Executors.newSingleThreadExecutor());

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
            JSONObject o = json.getJSONObject("hookUrl");
            if (o!=null && !o.isNullObject()) {
                hookUrl = o.getString("url");
            } else {
                hookUrl = null;
            }
            credentials = req.bindJSONToList(Credential.class,json.get("credentials"));
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
    public static boolean ALLOW_HOOKURL_OVERRIDE = false;

    private static final Logger LOGGER = Logger.getLogger(GitHubPushTrigger.class.getName());

    private static final Pattern[] URL_PATTERNS = {
        Pattern.compile("git@github.com:([^/.]+)/([^/.]+).git"),
        Pattern.compile("https://[^/.]+@github.com/([^/.]+)/([^/.]+).git"),
        Pattern.compile("git://github.com/([^/.]+)/([^/.]+).git")
    };
}

package com.cloudbees.jenkins;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import hudson.Extension;
import hudson.Util;
import hudson.XmlFile;
import hudson.console.AnnotatedLargeText;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Project;
import hudson.triggers.SCMTrigger;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.NamingThreadFactory;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.scm.api.SCMEvent;
import jenkins.triggers.SCMTriggerItem;
import jenkins.triggers.SCMTriggerItem.SCMTriggerItems;
import org.apache.commons.jelly.XMLOutput;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.admin.GitHubHookRegisterProblemMonitor;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.internal.GHPluginConfigException;
import org.jenkinsci.plugins.github.migration.Migrator;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.Validate.notNull;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.asParameterizedJobMixIn;

/**
 * Triggers a build when we receive a GitHub post-commit webhook.
 *
 * @author Kohsuke Kawaguchi
 */
public class GitHubPushTrigger extends Trigger<Job<?, ?>> implements GitHubTrigger {

    @DataBoundConstructor
    public GitHubPushTrigger() {
    }

    /**
     * Called when a POST is made.
     */
    @Deprecated
    public void onPost() {
        onPost(GitHubTriggerEvent.create()
                .build()
        );
    }

    /**
     * Called when a POST is made.
     */
    public void onPost(String triggeredByUser) {
        onPost(GitHubTriggerEvent.create()
                .withOrigin(SCMEvent.originOf(Stapler.getCurrentRequest()))
                .withTriggeredByUser(triggeredByUser)
                .build()
        );
    }

    /**
     * Called when a POST is made.
     */
    public void onPost(final GitHubTriggerEvent event) {
        if (Objects.isNull(job)) {
            return; // nothing to do
        }

        Job<?, ?> currentJob = notNull(job, "Job can't be null");

        final String pushBy = event.getTriggeredByUser();
        DescriptorImpl d = getDescriptor();
        d.checkThreadPoolSizeAndUpdateIfNecessary();
        d.queue.execute(new Runnable() {
            private boolean runPolling() {
                try {
                    StreamTaskListener listener = new StreamTaskListener(getLogFileForJob(currentJob));

                    try {
                        PrintStream logger = listener.getLogger();

                        long start = System.currentTimeMillis();
                        logger.println("Started on " + DateFormat.getDateTimeInstance().format(new Date()));
                        if (event.getOrigin() != null) {
                            logger.format("Started by event from %s on %tc%n", event.getOrigin(), event.getTimestamp());
                        }
                        SCMTriggerItem item = SCMTriggerItems.asSCMTriggerItem(currentJob);
                        if (null == item) {
                            throw new IllegalStateException("Job is not an SCMTriggerItem: " + currentJob);
                        }
                        boolean result = item.poll(listener).hasChanges();
                        logger.println("Done. Took " + Util.getTimeSpanString(System.currentTimeMillis() - start));
                        if (result) {
                            logger.println("Changes found");
                        } else {
                            logger.println("No changes");
                        }
                        return result;
                    } catch (Error e) {
                        e.printStackTrace(listener.error("Failed to record SCM polling"));
                        LOGGER.error("Failed to record SCM polling", e);
                        throw e;
                    } catch (RuntimeException e) {
                        e.printStackTrace(listener.error("Failed to record SCM polling"));
                        LOGGER.error("Failed to record SCM polling", e);
                        throw e;
                    } finally {
                        listener.close();
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to record SCM polling", e);
                }
                return false;
            }

            public void run() {
                if (runPolling()) {
                    GitHubPushCause cause;
                    try {
                        cause = new GitHubPushCause(getLogFileForJob(currentJob), pushBy);
                    } catch (IOException e) {
                        LOGGER.warn("Failed to parse the polling log", e);
                        cause = new GitHubPushCause(pushBy);
                    }

                    if (asParameterizedJobMixIn(currentJob).scheduleBuild(cause)) {
                        LOGGER.info("SCM changes detected in " + currentJob.getFullName()
                                + ". Triggering #" + currentJob.getNextBuildNumber());
                    } else {
                        LOGGER.info("SCM changes detected in " + currentJob.getFullName()
                                + ". Job is already in the queue");
                    }
                }
            }
        });
    }

    /**
     * Returns the file that records the last/current polling activity.
     */
    public File getLogFile() {
        try {
            return getLogFileForJob(notNull(job, "Job can't be null!"));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Returns the file that records the last/current polling activity.
     */
    private File getLogFileForJob(@Nonnull Job job) throws IOException {
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
    public void start(Job<?, ?> project, boolean newInstance) {
        super.start(project, newInstance);
        if (newInstance && GitHubPlugin.configuration().isManageHooks()) {
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

        if (GitHubPlugin.configuration().isManageHooks()) {
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
        public Job<?, ?> getOwner() {
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
            return Util.loadFile(getLogFileForJob(job));
        }

        /**
         * Writes the annotated log to the given output.
         *
         * @since 1.350
         */
        public void writeLogTo(XMLOutput out) throws IOException {
            new AnnotatedLargeText<GitHubWebHookPollingAction>(getLogFileForJob(job), Charsets.UTF_8, true, this)
                    .writeHtmlTo(0, out.asWriter());
        }
    }

    @Extension
    @Symbol("githubPush")
    public static class DescriptorImpl extends TriggerDescriptor {
        private final transient SequentialExecutionQueue queue =
                new SequentialExecutionQueue(Executors.newSingleThreadExecutor(threadFactory()));

        private transient String hookUrl;

        private transient List<Credential> credentials;

        @Inject
        private transient GitHubHookRegisterProblemMonitor monitor;

        @Inject
        private transient SCMTrigger.DescriptorImpl scmTrigger;

        private transient int maximumThreads = Integer.MIN_VALUE;

        public DescriptorImpl() {
            checkThreadPoolSizeAndUpdateIfNecessary();
        }

        /**
         * Update the {@link java.util.concurrent.ExecutorService} instance.
         */
        /*package*/
        synchronized void checkThreadPoolSizeAndUpdateIfNecessary() {
            if (scmTrigger != null) {
                int count = scmTrigger.getPollingThreadCount();
                if (maximumThreads != count) {
                    maximumThreads = count;
                    queue.setExecutors(
                            (count == 0
                                    ? Executors.newCachedThreadPool(threadFactory())
                                    : Executors.newFixedThreadPool(maximumThreads, threadFactory())));
                }
            }
        }

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof Job && SCMTriggerItems.asSCMTriggerItem(item) != null
                    && item instanceof ParameterizedJobMixIn.ParameterizedJob;
        }

        @Override
        public String getDisplayName() {
            return "GitHub hook trigger for GITScm polling";
        }

        /**
         * True if Jenkins should auto-manage hooks.
         *
         * @deprecated Use {@link GitHubPluginConfig#isManageHooks()} instead
         */
        @Deprecated
        public boolean isManageHook() {
            return GitHubPlugin.configuration().isManageHooks();
        }

        /**
         * Returns the URL that GitHub should post.
         *
         * @deprecated use {@link GitHubPluginConfig#getHookUrl()} instead
         */
        @Deprecated
        public URL getHookUrl() throws GHPluginConfigException {
            return GitHubPlugin.configuration().getHookUrl();
        }

        /**
         * @return null after migration
         * @deprecated use {@link GitHubPluginConfig#getConfigs()} instead.
         */
        @Deprecated
        public List<Credential> getCredentials() {
            return credentials;
        }

        /**
         * Used only for migration
         *
         * @return null after migration
         * @deprecated use {@link GitHubPluginConfig#getHookUrl()}
         */
        @Deprecated
        public URL getDeprecatedHookUrl() {
            if (isEmpty(hookUrl)) {
                return null;
            }
            try {
                return new URL(hookUrl);
            } catch (MalformedURLException e) {
                LOGGER.warn("Malformed hook url skipped while migration ({})", e.getMessage());
                return null;
            }
        }

        /**
         * Used to cleanup after migration
         */
        public void clearDeprecatedHookUrl() {
            this.hookUrl = null;
        }

        /**
         * Used to cleanup after migration
         */
        public void clearCredentials() {
            this.credentials = null;
        }

        /**
         * @deprecated use {@link GitHubPluginConfig#isOverrideHookURL()}
         */
        @Deprecated
        public boolean hasOverrideURL() {
            return GitHubPlugin.configuration().isOverrideHookURL();
        }

        /**
         * Uses global xstream to enable migration alias used in
         * {@link Migrator#enableCompatibilityAliases()}
         */
        @Override
        protected XmlFile getConfigFile() {
            return new XmlFile(Jenkins.XSTREAM2, super.getConfigFile().getFile());
        }

        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }

        public static boolean allowsHookUrlOverride() {
            return ALLOW_HOOKURL_OVERRIDE;
        }

        private static ThreadFactory threadFactory() {
            return new NamingThreadFactory(Executors.defaultThreadFactory(), "GitHubPushTrigger");
        }

        /**
         * Checks that repo defined in this item is not in administrative monitor as failed to be registered.
         * If that so, shows warning with some instructions
         *
         * @param item - to check against. Should be not null and have at least one repo defined
         *
         * @return warning or empty string
         * @since 1.17.0
         */
        @SuppressWarnings("unused")
        @Restricted(NoExternalUse.class) // invoked from Stapler
        public FormValidation doCheckHookRegistered(@AncestorInPath Item item) {
            Preconditions.checkNotNull(item, "Item can't be null if wants to check hook in monitor");

            if (!item.hasPermission(Item.CONFIGURE)) {
                return FormValidation.ok();
            }

            Collection<GitHubRepositoryName> repos = GitHubRepositoryNameContributor.parseAssociatedNames(item);

            for (GitHubRepositoryName repo : repos) {
                if (monitor.isProblemWith(repo)) {
                    return FormValidation.warning(
                            org.jenkinsci.plugins.github.Messages.github_trigger_check_method_warning_details(
                                    repo.getUserName(), repo.getRepositoryName(), repo.getHost()
                            ));
                }
            }

            return FormValidation.ok();
        }
    }

    /**
     * Set to false to prevent the user from overriding the hook URL.
     */
    public static final boolean ALLOW_HOOKURL_OVERRIDE = !Boolean.getBoolean(
            GitHubPushTrigger.class.getName() + ".disableOverride"
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPushTrigger.class);
}

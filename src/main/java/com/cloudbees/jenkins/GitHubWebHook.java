package com.cloudbees.jenkins;

import com.google.common.base.Function;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Job;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;
import hudson.util.SequentialExecutionQueue;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.Validate;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.jenkinsci.plugins.github.internal.GHPluginConfigException;
import org.jenkinsci.plugins.github.webhook.GHEventHeader;
import org.jenkinsci.plugins.github.webhook.GHEventPayload;
import org.jenkinsci.plugins.github.webhook.GHSignatureHeader;
import org.jenkinsci.plugins.github.webhook.RequirePostWithGHHookPayload;
import org.kohsuke.github.GHEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URL;
import java.util.List;

import static hudson.model.Computer.threadPoolForRemoting;
import static org.apache.commons.lang3.Validate.notNull;
import static org.jenkinsci.plugins.github.extension.GHEventsSubscriber.isInterestedIn;
import static org.jenkinsci.plugins.github.extension.GHEventsSubscriber.processEvent;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isAlive;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isBuildable;
import static org.jenkinsci.plugins.github.webhook.WebhookManager.forHookUrl;


/**
 * Receives github hook.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class GitHubWebHook implements UnprotectedRootAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubWebHook.class);
    public static final String URLNAME = "github-webhook";

    // headers used for testing the endpoint configuration
    public static final String URL_VALIDATION_HEADER = "X-Jenkins-Validation";
    public static final String X_INSTANCE_IDENTITY = "X-Instance-Identity";

    private final transient SequentialExecutionQueue queue = new SequentialExecutionQueue(threadPoolForRemoting);

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return URLNAME;
    }

    /**
     * If any wants to auto-register hook, then should call this method
     * Example code:
     * {@code GitHubWebHook.get().registerHookFor(job);}
     *
     * @param job not null project to register hook for
     */
    public void registerHookFor(Job job) {
        reRegisterHookForJob().apply(job);
    }

    /**
     * Calls {@link #registerHookFor(Job)} for every project which have subscriber
     *
     * @return list of jobs which jenkins tried to register hook
     */
    public List<Job> reRegisterAllHooks() {
        return from(getJenkinsInstance().getAllItems(Job.class))
                .filter(isBuildable())
                .filter(isAlive())
                .transform(reRegisterHookForJob()).toList();
    }

    /**
     * Receives the webhook call
     *
     * @param event   GH event type. Never null
     * @param payload Payload from hook. Never blank
     * @param signature GH signature of the payload. Null if header is not present.
     */
    @SuppressWarnings("unused")
    @RequirePostWithGHHookPayload
    public void doIndex(@Nonnull @GHEventHeader GHEvent event, @Nonnull @GHEventPayload String payload, @Nullable @GHSignatureHeader String signature) {
        from(GHEventsSubscriber.all())
                .filter(isInterestedIn(event))
                .transform(processEvent(event, payload, signature)).toList();
    }

    private Function<Job, Job> reRegisterHookForJob() {
        return new Function<Job, Job>() {
            @Override
            public Job apply(Job job) {
                LOGGER.debug("Calling registerHooks() for {}", notNull(job, "Job can't be null").getFullName());

                // We should handle wrong url of self defined hook url here in any case with try-catch :(
                URL hookUrl;
                try {
                    hookUrl = GitHubPlugin.configuration().getHookUrl();
                } catch (GHPluginConfigException e) {
                    LOGGER.error("Skip registration of GHHook ({})", e.getMessage());
                    return job;
                }
                Runnable hookRegistrator = forHookUrl(hookUrl).registerFor(job);
                queue.execute(hookRegistrator);
                return job;
            }
        };
    }

    public static GitHubWebHook get() {
        return Jenkins.getInstance().getExtensionList(RootAction.class).get(GitHubWebHook.class);
    }

    @Nonnull
    public static Jenkins getJenkinsInstance() throws IllegalStateException {
        Jenkins instance = Jenkins.getInstance();
        Validate.validState(instance != null, "Jenkins has not been started, or was already shut down");
        return instance;
    }

    /**
     * Other plugins may be interested in listening for these updates.
     *
     * @since 1.8
     */
    public abstract static class Listener implements ExtensionPoint {

        /**
         * Called when there is a change notification on a specific repository.
         *
         * @param pusherName        the pusher name.
         * @param changedRepository the changed repository.
         *
         * @since 1.8
         */
        public abstract void onPushRepositoryChanged(String pusherName, GitHubRepositoryName changedRepository);
    }

}

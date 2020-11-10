package com.cloudbees.jenkins;

import com.google.common.base.Function;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;
import hudson.util.SequentialExecutionQueue;
import jenkins.model.Jenkins;
import jenkins.scm.api.SCMEvent;
import org.apache.commons.lang3.Validate;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.jenkinsci.plugins.github.internal.GHPluginConfigException;
import org.jenkinsci.plugins.github.webhook.GHEventHeader;
import org.jenkinsci.plugins.github.webhook.GHEventPayload;
import org.jenkinsci.plugins.github.webhook.RequirePostWithGHHookPayload;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.GHEvent;
import org.kohsuke.stapler.Stapler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.List;

import static hudson.model.Computer.threadPoolForRemoting;
import static org.apache.commons.lang3.Validate.notNull;
import static org.jenkinsci.plugins.github.extension.GHEventsSubscriber.isInterestedIn;
import static org.jenkinsci.plugins.github.extension.GHEventsSubscriber.processEvent;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isAlive;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isBuildable;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isNotSCMSourceOwner;
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
     * @deprecated use {@link #registerHookFor(Item)}
     */
    @Deprecated
    public void registerHookFor(Job job) {
        reRegisterHookForJob().apply(job);
    }

    /**
     * If any wants to auto-register hook, then should call this method
     * Example code:
     * {@code GitHubWebHook.get().registerHookFor(item);}
     *
     * @param item not null item to register hook for
     * @since 1.25.0
     */
    public void registerHookFor(Item item) {
        reRegisterHookForJob().apply(item);
    }

    /**
     * Calls {@link #registerHookFor(Job)} for every project which have subscriber
     *
     * @return list of jobs which jenkins tried to register hook
     */
    public List<Item> reRegisterAllHooks() {
        return from(getJenkinsInstance().getAllItems(Item.class))
                .filter(isBuildable())
                .filter(isAlive())
                .filter(isNotSCMSourceOwner())
                .transform(reRegisterHookForJob())
                .toList();
    }

    /**
     * Receives the webhook call
     *
     * @param event   GH event type. Never null
     * @param payload Payload from hook. Never blank
     */
    @SuppressWarnings("unused")
    @RequirePostWithGHHookPayload
    public void doIndex(@Nonnull @GHEventHeader GHEvent event, @Nonnull @GHEventPayload String payload) {
        GHSubscriberEvent subscriberEvent =
                new GHSubscriberEvent(SCMEvent.originOf(Stapler.getCurrentRequest()), event, payload);
        from(GHEventsSubscriber.all())
                .filter(isInterestedIn(event))
                .transform(processEvent(subscriberEvent)).toList();
    }

    private <T extends Item> Function<T, T> reRegisterHookForJob() {
        return new Function<T, T>() {
            @Override
            public T apply(T job) {
                LOGGER.debug("Calling registerHooks() for {}", notNull(job, "Item can't be null").getFullName());

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
     * @deprecated working theory is that this API is not required any more with the {@link SCMEvent} based API,
     * if wrong, please raise a JIRA
     */
    @Deprecated
    @Restricted(NoExternalUse.class)
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

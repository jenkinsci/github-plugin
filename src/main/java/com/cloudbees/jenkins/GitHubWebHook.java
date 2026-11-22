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

import edu.umd.cs.findbugs.annotations.NonNull;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jenkinsci.plugins.github.webhook.WebhookManager;
import org.jenkinsci.plugins.github.webhook.WebhookManager.RegistrationOutcome;

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
    /**
     * X-GitHub-Delivery: A globally unique identifier (GUID) to identify the event.
     * @see <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads#delivery-headers">Delivery
     * headers</a>
     */
    public static final String X_GITHUB_DELIVERY = "X-GitHub-Delivery";

    private final transient SequentialExecutionQueue queue = new SequentialExecutionQueue(threadPoolForRemoting);
    private transient volatile BulkRegistrationStatus bulkRegistrationStatus = BulkRegistrationStatus.empty();
    private transient boolean bulkRegistrationPending;

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
     * Schedules a bulk refresh. A single task processes each unique repository once,
     * merging events from all interested jobs. Repeated clicks cannot enqueue overlapping
     * bulk runs. Existing webhooks are refreshed so changes to their secrets take effect.
     *
     * @return the participating Jenkins items; this is not a success count
     */
    public synchronized List<Item> reRegisterAllHooks() {
        List<Item> items = from(getJenkinsInstance().getAllItems(Item.class))
                .filter(isBuildable())
                .filter(isAlive())
                .toList();

        if (bulkRegistrationPending) {
            return items;
        }

        Map<GitHubRepositoryName, Set<GHEvent>> grouped = new LinkedHashMap<>();
        for (Item item : items) {
            Set<GHEvent> events = new LinkedHashSet<>();
            from(GHEventsSubscriber.all())
                    .filter(GHEventsSubscriber.isApplicableFor(item))
                    .transformAndConcat(GHEventsSubscriber.extractEvents())
                    .toList().forEach(events::add);
            if (events.isEmpty()) {
                continue;
            }
            for (GitHubRepositoryName name : GitHubRepositoryNameContributor.parseAssociatedNames(item)) {
                grouped.computeIfAbsent(name, ignored -> new LinkedHashSet<>()).addAll(events);
            }
        }

        URL hookUrl;
        try {
            hookUrl = GitHubPlugin.configuration().getHookUrl();
        } catch (GHPluginConfigException e) {
            LOGGER.error("Skip registration of GHHook ({})", e.getMessage());
            bulkRegistrationStatus = BulkRegistrationStatus.failedConfiguration(grouped.size());
            return items;
        }

        BulkRegistrationStatus status = new BulkRegistrationStatus(grouped.size());
        bulkRegistrationStatus = status;
        bulkRegistrationPending = true;
        try {
            queue.execute(() -> {
                try {
                    WebhookManager manager = forHookUrl(hookUrl);
                    grouped.forEach((name, events) -> {
                        RegistrationOutcome result = manager.registerRepository(name,
                                new ArrayList<>(events), true);
                        status.record(result);
                    });
                } finally {
                    status.complete();
                    synchronized (GitHubWebHook.this) {
                        bulkRegistrationPending = false;
                    }
                }
            });
        } catch (RuntimeException e) {
            bulkRegistrationPending = false;
            status.failToQueue();
            throw e;
        }
        return items;
    }

    public BulkRegistrationStatus getBulkRegistrationStatus() {
        return bulkRegistrationStatus;
    }

    /**
     * A progress snapshot is obtained through getters; atomic counters allow polling while
     * the sequential queue processes the batch.
     */
    public static final class BulkRegistrationStatus {
        private final int total;
        private final AtomicInteger created = new AtomicInteger();
        private final AtomicInteger skipped = new AtomicInteger();
        private final AtomicInteger failed = new AtomicInteger();
        private volatile boolean completed;

        private BulkRegistrationStatus(int total) {
            this.total = total;
        }

        static BulkRegistrationStatus empty() {
            BulkRegistrationStatus result = new BulkRegistrationStatus(0);
            result.complete();
            return result;
        }

        static BulkRegistrationStatus failedConfiguration(int total) {
            BulkRegistrationStatus result = new BulkRegistrationStatus(total);
            result.failed.set(total);
            result.complete();
            return result;
        }

        private void record(RegistrationOutcome outcome) {
            switch (outcome) {
                case CREATED:
                    created.incrementAndGet();
                    break;
                case ALREADY_REGISTERED:
                    skipped.incrementAndGet();
                    break;
                case FAILED:
                    failed.incrementAndGet();
                    break;
                default:
                    throw new IllegalStateException("Unknown registration outcome: " + outcome);
            }
        }

        private void complete() {
            completed = true;
        }

        private void failToQueue() {
            failed.set(total);
            complete();
        }

        public int getTotal() {
            return total;
        }

        public int getCreated() {
            return created.get();
        }

        public int getSkipped() {
            return skipped.get();
        }

        public int getFailed() {
            return failed.get();
        }

        public int getPending() {
            return total - getCreated() - getSkipped() - getFailed();
        }

        public boolean isCompleted() {
            return completed;
        }
    }

    /**
     * Receives the webhook call
     *
     * @param event   GH event type. Never null
     * @param payload Payload from hook. Never blank
     */
    @SuppressWarnings("unused")
    @RequirePostWithGHHookPayload
    public void doIndex(@NonNull @GHEventHeader GHEvent event, @NonNull @GHEventPayload String payload) {
        var currentRequest = Stapler.getCurrentRequest2();
        String eventGuid = currentRequest.getHeader(X_GITHUB_DELIVERY);
        GHSubscriberEvent subscriberEvent =
                new GHSubscriberEvent(eventGuid, SCMEvent.originOf(currentRequest), event, payload);
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

    @NonNull
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

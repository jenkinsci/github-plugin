package org.jenkinsci.plugins.github.webhook.subscriber;

import static com.google.common.collect.Sets.immutableEnumSet;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.PeriodicWork;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.kohsuke.github.GHEvent;

@Extension
public final class DuplicateEventsSubscriber extends GHEventsSubscriber {

    private static final Logger LOGGER = Logger.getLogger(DuplicateEventsSubscriber.class.getName());
    private static final Map<String, EventCountWithTTL> EVENT_COUNTS_TRACKER = new ConcurrentHashMap<>();
    private static final long TTL_MILLIS = TimeUnit.MINUTES.toMillis(10);

    @VisibleForTesting
    record EventCountWithTTL(int count, long lastUpdated) { }

    /**
     * This method is retained only because it is an abstract method.
     * It is no longer used to determine event delivery to subscribers.
     * Instead, {@link #isInterestedIn} and {@link #events()} are now used to
     * decide whether an event should be delivered to a subscriber.
     * @see com.cloudbees.jenkins.GitHubWebHook#doIndex
     */
    @Override
    protected boolean isApplicable(@Nullable Item item) {
        return false;
    }

    /**
     * Subscribe to events that can trigger some kind of action within Jenkins, such as repository scan, build launch,
     * etc.
     * <p>
     * There are about 63 specific events mentioned in the {@link GHEvent} enum, but not all of them are useful in
     * Jenkins. Subscribing to and tracking them in duplicates tracker would cause an increase in memory usage, and
     * those events' occurrences are likely larger than those that cause an action in Jenkins.
     * <p>
     * <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads">
     *     Documentation reference (as also referenced in {@link GHEvent})</a>
     * */
    @Override
    protected Set<GHEvent> events() {
        return immutableEnumSet(
                GHEvent.CHECK_RUN, // associated with GitHub action Re-run button to trigger build
                GHEvent.CHECK_SUITE, // associated with GitHub action Re-run button to trigger build
                GHEvent.CREATE, // branch or tag creation
                GHEvent.DELETE, // branch or tag deletion
                GHEvent.PULL_REQUEST, // PR creation (also PR close or merge)
                GHEvent.PUSH // commit push
            );
    }

    @Override
    protected void onEvent(final GHSubscriberEvent event) {
        String eventGuid = event.getEventGuid();
        if (eventGuid == null) {
            return;
        }
        long now = Instant.now().toEpochMilli();
        EVENT_COUNTS_TRACKER.compute(
                eventGuid, (key, value) -> new EventCountWithTTL(value == null ? 1 : value.count() + 1, now));
    }

    public static Map<String, Integer> getDuplicateEventCounts() {
        return EVENT_COUNTS_TRACKER.entrySet().stream()
                .filter(entry -> entry.getValue().count() > 1)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().count()));
    }

    @VisibleForTesting
    static void cleanUpOldEntries(long now) {
        EVENT_COUNTS_TRACKER
                .entrySet()
                .removeIf(entry -> (now - entry.getValue().lastUpdated()) > TTL_MILLIS);
    }

    /**
     * Only for testing purpose
     */
    @VisibleForTesting
    static Map<String, EventCountWithTTL> getEventCountsTracker() {
        return new ConcurrentHashMap<>(EVENT_COUNTS_TRACKER);
    }

    @SuppressWarnings("unused")
    @Extension
    public static class EventCountTrackerCleanup extends PeriodicWork {

        /**
         * At present, as the {@link #TTL_MILLIS} is set to 10 minutes, we consider half of it for cleanup.
         * This recurrence period is chosen to balance removing stale entries from accumulating in memory vs.
         * additional load on Jenkins due to a new periodic job execution.
         * <p>
         * If we want to keep the stale entries to a minimum, there appear to be three different ways to achieve this:
         * <ul>
         *     <li>Increasing the frequency of this periodic task, which will contribute to load</li>
         *     <li>Event-driven cleanup: for every event from GH, clean up expired entries (need to use
         *     better data structures and algorithms; simply calling the current {@link #cleanUpOldEntries} will
         *     result in {@code O(n)} for every {@code insert}, which may lead to slowness in this hot code path)</li>
         *     <li>Adaptive cleanup: based on the number of stale entries being seen, the system itself will adjust
         *     the periodic task's frequency (if such adaptive scheduling does not already exist in Jenkins core,
         *     this wouldn't be a good idea to implement here)
         *     </li>
         * </ul>
         */
        @Override
        public long getRecurrencePeriod() {
            return TTL_MILLIS / 2;
        }

        @Override
        protected void doRun() {
            LOGGER.log(
                    Level.FINE,
                    () -> "Cleaning up entries older than " + TTL_MILLIS + "ms, remaining entries: "
                            + EVENT_COUNTS_TRACKER.size());
            cleanUpOldEntries(Instant.now().toEpochMilli());
        }
    }
}

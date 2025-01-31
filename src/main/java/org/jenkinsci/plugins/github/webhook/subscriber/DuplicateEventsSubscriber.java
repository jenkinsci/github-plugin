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

    @Override
    protected Set<GHEvent> events() {
        return immutableEnumSet(GHEvent.PUSH);
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
        cleanUpOldEntries(now);
    }

    public static Map<String, Integer> getDuplicateEventCounts() {
        return EVENT_COUNTS_TRACKER.entrySet().stream()
                .filter(entry -> entry.getValue().count() > 1)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue().count()));
    }

    private static void cleanUpOldEntries(long now) {
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

        @Override
        public long getRecurrencePeriod() {
            return TTL_MILLIS;
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

package org.jenkinsci.plugins.github.webhook.subscriber;

import static com.google.common.collect.Sets.immutableEnumSet;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.PeriodicWork;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.GHEvent;

/**
 * Tracks duplicate {@link GHEvent} triggering actions in Jenkins.
 * Events are tracked for 10 minutes, with the last detected duplicate reference retained for up to 24 hours
 * (see {@link #isDuplicateEventSeen}).
 * <p>
 * Duplicates are stored in-memory only, so a controller restart clears all entries as if none existed.
 * Persistent storage is omitted for simplicity, since webhook misconfigurations would likely cause new duplicates.
 */
@Extension
public final class DuplicateEventsSubscriber extends GHEventsSubscriber {

    private static final Logger LOGGER = Logger.getLogger(DuplicateEventsSubscriber.class.getName());
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Duration TWENTY_FOUR_HOURS = Duration.ofHours(24);
    /**
     * Stores the event GUID and the time it was last seen.
     */
    private static final Map<String, Instant> EVENT_TRACKER = new ConcurrentHashMap<>();
    private static volatile TrackedDuplicateEvent lastDuplicate;

    private static Clock clock = Clock.systemUTC();
    public record TrackedDuplicateEvent(String eventGuid, Instant lastUpdated, GHSubscriberEvent ghSubscriberEvent) { }

    @VisibleForTesting
    @Restricted(NoExternalUse.class)
    void setClock(Clock clock) {
        DuplicateEventsSubscriber.clock = clock;
    }

    /**
     * This subscriber is not applicable to any item
     *
     * @param item ignored
     * @return always false
     */
    @Override
    protected boolean isApplicable(@Nullable Item item) {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Subscribes to events that trigger actions in Jenkins, such as repository scans or builds.
     * <p>
     * The {@link GHEvent} enum defines about 63 events, but not all are relevant to Jenkins.
     * Tracking unnecessary events increases memory usage, and they occur more frequently than those triggering any
     * work.
     * <p>
     * <a href="https://docs.github.com/en/webhooks/webhook-events-and-payloads">
     *     Documentation reference (also referenced in {@link GHEvent})</a>
     */
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
        LOGGER.fine(() -> "Received event with GUID: " + eventGuid);
        if (eventGuid == null) {
            return;
        }
        if (EVENT_TRACKER.containsKey(eventGuid)) {
            lastDuplicate = new TrackedDuplicateEvent(eventGuid, Instant.now(clock), event);
        }
        EVENT_TRACKER.put(eventGuid, Instant.now(clock));
    }

    /**
     * Checks if a duplicate event was recorded in the past 24 hours.
     * <p>
     * Events are not stored for 24 hours—only the most recent duplicate is checked within this timeframe.
     *
     * @return {@code true} if a duplicate was seen in the last 24 hours, {@code false} otherwise.
     */
    public static boolean isDuplicateEventSeen() {
        return lastDuplicate != null
               && Duration.between(lastDuplicate.lastUpdated(), Instant.now(clock)).compareTo(TWENTY_FOUR_HOURS) < 0;
    }

    public static TrackedDuplicateEvent getLastDuplicate() {
        return lastDuplicate;
    }

    @VisibleForTesting
    @Restricted(NoExternalUse.class)
    static void cleanUpOldEntries() {
        var now = Instant.now(clock);
        EVENT_TRACKER.entrySet().removeIf(entry -> Duration.between(entry.getValue(), now).compareTo(TTL) > 0);
        LOGGER.fine(() -> "Entries remaining after cleanup " + EVENT_TRACKER.size());
    }

    @VisibleForTesting
    @Restricted(NoExternalUse.class)
    static Map<String, Instant> getEventCountsTracker() {
        return Collections.unmodifiableMap(EVENT_TRACKER);
    }

    /**
     * Periodically runs every 5 minutes, and remove old entries from {@link #EVENT_TRACKER}.
     */
    @SuppressWarnings("unused")
    @Extension
    public static class EventCountBackgroundWork extends PeriodicWork {

        @Override
        public long getRecurrencePeriod() {
            return TTL.toMillis() / 2;
        }

        @Override
        protected void doRun() {
            LOGGER.fine(() -> "Cleaning up old entries from duplicate events tracker");
            cleanUpOldEntries();
        }
    }
}

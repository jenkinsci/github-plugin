package org.jenkinsci.plugins.github.webhook.subscriber;

import static com.google.common.collect.Sets.immutableEnumSet;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.model.Item;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

    private static Ticker ticker = Ticker.systemTicker();
    /**
     * Caches GitHub event GUIDs for 10 minutes to track recent events to detect duplicates.
     * <p>
     * Only the keys (event GUIDs) are relevant, as Caffeine automatically handles expiration based
     * on insertion time; the value is irrelevant, we put {@link #DUMMY}, as Caffeine doesn't provide any
     * Set structures.
     * <p>
     * Maximum cache size is set to 24k so it doesn't grow unbound (approx. 1MB). Each key takes 36 bytes, and
     * timestamp (assuming caffeine internally keeps long) takes 8 bytes; total of 44 bytes
     * per entry. So the maximum memory consumed by this cache is 24k * 44 = 1056k = 1.056 MB.
     */
    private static final Cache<String, Object> EVENT_TRACKER = Caffeine.newBuilder()
                                                                       .maximumSize(24_000L)
                                                                       .expireAfterWrite(Duration.ofMinutes(10))
                                                                       .ticker(() -> ticker.read())
                                                                       .build();
    private static final Object DUMMY = new Object();

    private static volatile TrackedDuplicateEvent lastDuplicate;
    public record TrackedDuplicateEvent(String eventGuid, Instant lastUpdated, GHSubscriberEvent ghSubscriberEvent) { }
    private static final Duration TWENTY_FOUR_HOURS = Duration.ofHours(24);

    @VisibleForTesting
    @Restricted(NoExternalUse.class)
    static void setTicker(Ticker testTicker) {
        ticker = testTicker;
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
        if (EVENT_TRACKER.getIfPresent(eventGuid) != null) {
            lastDuplicate = new TrackedDuplicateEvent(eventGuid, getNow(), event);
        }
        EVENT_TRACKER.put(eventGuid, DUMMY);
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
               && Duration.between(lastDuplicate.lastUpdated(), getNow()).compareTo(TWENTY_FOUR_HOURS) < 0;
    }

    private static Instant getNow() {
        return Instant.ofEpochSecond(0L, ticker.read());
    }

    public static TrackedDuplicateEvent getLastDuplicate() {
        return lastDuplicate;
    }

    /**
     * Caffeine expired keys are not removed immediately. Method returns the non-expired keys; required for the tests.
     */
    @VisibleForTesting
    @Restricted(NoExternalUse.class)
    static Set<String> getEventCountsTracker() {
        return EVENT_TRACKER.asMap().keySet().stream()
                            .filter(key -> EVENT_TRACKER.getIfPresent(key) != null)
                            .collect(Collectors.toSet());
    }
}

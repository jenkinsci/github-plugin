package org.jenkinsci.plugins.github.webhook.subscriber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.junit.Before;
import org.junit.Test;
import org.kohsuke.github.GHEvent;
import org.mockito.Mockito;


public class DuplicateEventsSubscriberTest {

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        // make sure the static hashmap is empty
        Field eventCountTracker = DuplicateEventsSubscriber.class.getDeclaredField("EVENT_COUNTS_TRACKER");
        eventCountTracker.setAccessible(true);
        ((Map<String, DuplicateEventsSubscriber.EventCountWithTTL>) eventCountTracker.get(DuplicateEventsSubscriber.class)).clear();
    }

    @Test
    public void shouldReturnEventsWithCountMoreThanOne() {
        var subscriber = new DuplicateEventsSubscriber();
        subscriber.onEvent(new GHSubscriberEvent("1", "origin", GHEvent.PUSH, "payload"));
        subscriber.onEvent(new GHSubscriberEvent("1", "origin", GHEvent.PUSH, "payload"));
        subscriber.onEvent(new GHSubscriberEvent("2", "origin", GHEvent.PUSH, "payload"));
        subscriber.onEvent(new GHSubscriberEvent("3", "origin", GHEvent.PUSH, "payload"));
        assertThat(DuplicateEventsSubscriber.getDuplicateEventCounts(), is(Map.of("1", 2)));
        assertThat(DuplicateEventsSubscriber.getEventCountsTracker().keySet(), is(Set.of("1", "2", "3")));

        // also notice the `null` guid event is ignored
        subscriber.onEvent(new GHSubscriberEvent(null, "origin", GHEvent.PUSH, "payload"));
        assertThat(DuplicateEventsSubscriber.getEventCountsTracker().keySet(), is(Set.of("1", "2", "3")));
    }

    @Test
    public void shouldCleanupEventsOlderThanTTLMills() {
        var subscriber = new DuplicateEventsSubscriber();
        Instant past = OffsetDateTime.parse("2021-01-01T00:00:00Z").toInstant();
        Instant later = OffsetDateTime.parse("2021-01-01T11:00:00Z").toInstant();
        try (var mockedInstant = Mockito.mockStatic(Instant.class)) {
            mockedInstant.when(Instant::now).thenReturn(past);
            // add two events in `past` instant
            subscriber.onEvent(new GHSubscriberEvent("1", "origin", GHEvent.PUSH, "payload"));
            subscriber.onEvent(new GHSubscriberEvent("2", "origin", GHEvent.PUSH, "payload"));
            assertThat(DuplicateEventsSubscriber.getEventCountsTracker().size(), is(2));

            // add a new event in `later` instant
            mockedInstant.when(Instant::now).thenReturn(later);
            subscriber.onEvent(new GHSubscriberEvent("3", "origin", GHEvent.PUSH, "payload"));

            // run the cleanup
            DuplicateEventsSubscriber.cleanUpOldEntries(later.toEpochMilli());

            // assert only the new event is present, and old events are cleaned up
            var tracker = DuplicateEventsSubscriber.getEventCountsTracker();
            assertThat(tracker.size(), is(1));
            assertThat(tracker.get("3").count(), is(1));
            assertThat(tracker.get("3").lastUpdated(), is(later.toEpochMilli()));
        }
    }
}

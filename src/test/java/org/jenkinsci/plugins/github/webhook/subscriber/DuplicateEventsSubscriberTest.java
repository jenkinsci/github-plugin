package org.jenkinsci.plugins.github.webhook.subscriber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.jenkinsci.plugins.github.webhook.subscriber.DuplicateEventsSubscriber.getEventCountsTracker;
import static org.jenkinsci.plugins.github.webhook.subscriber.DuplicateEventsSubscriber.getLastDuplicate;
import static org.jenkinsci.plugins.github.webhook.subscriber.DuplicateEventsSubscriber.isDuplicateEventsSeen;

import java.time.Instant;
import java.util.Set;

import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.junit.Test;
import org.kohsuke.github.GHEvent;
import org.mockito.Mockito;

public class DuplicateEventsSubscriberTest {

    @Test
    public void onEventShouldTrackEventAndKeepTrackOfLastDuplicate() {
        var subscriber = new DuplicateEventsSubscriber();
        assertThat("lastDuplicate is null at first", getLastDuplicate(), is(nullValue()));
        assertThat("should not throw NPE", isDuplicateEventsSeen(), is(false));
        // send a null event
        subscriber.onEvent(new GHSubscriberEvent(null, "origin", GHEvent.PUSH, "payload"));
        assertThat("null event is not tracked", getEventCountsTracker().size(), is(0));
        assertThat("lastDuplicate is still null", getLastDuplicate(), is(nullValue()));

        var now = Instant.parse("2025-02-05T03:00:00Z");
        var after1Sec = Instant.parse("2025-02-05T03:00:01Z");
        var after2Sec = Instant.parse("2025-02-05T03:00:02Z");
        var after24Hour1Sec = Instant.parse("2025-02-06T03:00:03Z");

        try (var mockedInstant = Mockito.mockStatic(Instant.class)) {
            // first occurrence of events.
            mockedInstant.when(Instant::now).thenReturn(now);
            subscriber.onEvent(new GHSubscriberEvent("1", "origin", GHEvent.PUSH, "payload"));
            assertThat(getEventCountsTracker().keySet(), is(Set.of("1")));
            assertThat(getLastDuplicate(), is(nullValue()));
            assertThat(isDuplicateEventsSeen(), is(false));
            subscriber.onEvent(new GHSubscriberEvent("2", "origin", GHEvent.PUSH, "payload"));
            assertThat(getLastDuplicate(), is(nullValue()));
            assertThat(isDuplicateEventsSeen(), is(false));
            assertThat(getEventCountsTracker().keySet(), is(Set.of("1", "2")));
            subscriber.onEvent(new GHSubscriberEvent(null, "origin", GHEvent.PUSH, "payload"));
            assertThat(getLastDuplicate(), is(nullValue()));
            assertThat(getEventCountsTracker().keySet(), is(Set.of("1", "2")));
            assertThat(isDuplicateEventsSeen(), is(false));

            // second occurrence happens after 1 second
            mockedInstant.when(Instant::now).thenReturn(after1Sec);
            subscriber.onEvent(new GHSubscriberEvent("1", "origin", GHEvent.PUSH, "payload"));
            assertThat(getLastDuplicate().eventGuid(), is("1"));
            assertThat(getLastDuplicate().lastUpdated(), is(after1Sec.toEpochMilli()));
            assertThat(getEventCountsTracker().keySet(), is(Set.of("1", "2")));
            assertThat(isDuplicateEventsSeen(), is(true));

            // second occurrence for another event after 2 seconds
            mockedInstant.when(Instant::now).thenReturn(after2Sec);
            subscriber.onEvent(new GHSubscriberEvent("2", "origin", GHEvent.PUSH, "payload"));
            assertThat(getLastDuplicate().eventGuid(), is("2"));
            assertThat(getLastDuplicate().lastUpdated(), is(after2Sec.toEpochMilli()));
            assertThat(getEventCountsTracker().keySet(), is(Set.of("1", "2")));
            assertThat(isDuplicateEventsSeen(), is(true));

            // 24 hours has passed
            mockedInstant.when(Instant::now).thenReturn(after24Hour1Sec);
            assertThat(isDuplicateEventsSeen(), is(false));
        }
    }

    @Test
    public void cleanUpOldEntriesShouldClearEventsOlderThanTTL() {
        var subscriber = new DuplicateEventsSubscriber();
        var now = Instant.parse("2025-02-05T03:00:00Z");
        var after2Min = Instant.parse("2025-02-05T03:02:00Z");
        var after10Min1Sec = Instant.parse("2025-02-05T03:10:01Z");
        try (var mockedInstant = Mockito.mockStatic(Instant.class)) {
            mockedInstant.when(Instant::now).thenReturn(now);
            subscriber.onEvent(new GHSubscriberEvent("1", "origin", GHEvent.PUSH, "payload"));
            subscriber.onEvent(new GHSubscriberEvent("2", "origin", GHEvent.PUSH, "payload"));
            assertThat(getEventCountsTracker().size(), is(2));
            DuplicateEventsSubscriber.cleanUpOldEntries();
            assertThat(getEventCountsTracker().size(), is(2));

            // events after 2 minutes
            mockedInstant.when(Instant::now).thenReturn(after2Min);
            subscriber.onEvent(new GHSubscriberEvent("3", "origin", GHEvent.PUSH, "payload"));
            subscriber.onEvent(new GHSubscriberEvent("4", "origin", GHEvent.PUSH, "payload"));
            assertThat(getEventCountsTracker().size(), is(4));
            DuplicateEventsSubscriber.cleanUpOldEntries();
            assertThat(getEventCountsTracker().size(), is(4));

            // 10 minutes later
            mockedInstant.when(Instant::now).thenReturn(after10Min1Sec);
            DuplicateEventsSubscriber.cleanUpOldEntries();
            assertThat(getEventCountsTracker().size(), is(2));
            assertThat(getEventCountsTracker().keySet(), is(Set.of("3", "4")));
            assertThat(getEventCountsTracker().get("3"), is(after2Min.toEpochMilli()));
            assertThat(getEventCountsTracker().get("4"), is(after2Min.toEpochMilli()));
        }
    }
}

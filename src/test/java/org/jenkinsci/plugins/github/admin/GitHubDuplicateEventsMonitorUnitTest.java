package org.jenkinsci.plugins.github.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.github.benmanes.caffeine.cache.Ticker;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.junit.Test;
import org.jvnet.hudson.test.For;
import org.kohsuke.github.GHEvent;

@For(GitHubDuplicateEventsMonitor.DuplicateEventsSubscriber.class)
public class GitHubDuplicateEventsMonitorUnitTest {

    @Test
    public void onEventShouldTrackEventAndKeepTrackOfLastDuplicate() {
        var subscriber = new GitHubDuplicateEventsMonitor.DuplicateEventsSubscriber();

        var now = Instant.parse("2025-02-05T03:00:00Z");
        var after1Sec = Instant.parse("2025-02-05T03:00:01Z");
        var after2Sec = Instant.parse("2025-02-05T03:00:02Z");
        FakeTicker fakeTicker = new FakeTicker(now);
        subscriber.setTicker(fakeTicker);

        assertThat("lastDuplicate is null at first", subscriber.getLastDuplicate(), is(nullValue()));
        assertThat("should not throw NPE", subscriber.isDuplicateEventSeen(), is(false));
        // send a null event
        subscriber.onEvent(new GHSubscriberEvent(null, "origin", GHEvent.PUSH, "payload"));
        assertThat("null event is not tracked", subscriber.getPresentEventKeys().size(), is(0));
        assertThat("lastDuplicate is still null", subscriber.getLastDuplicate(), is(nullValue()));

        // at present
        subscriber.onEvent(new GHSubscriberEvent("1", "origin", GHEvent.PUSH, "payload"));
        assertThat(subscriber.getPresentEventKeys(), is(Set.of("1")));
        assertThat(subscriber.getLastDuplicate(), is(nullValue()));
        assertThat(subscriber.isDuplicateEventSeen(), is(false));
        subscriber.onEvent(new GHSubscriberEvent("2", "origin", GHEvent.PUSH, "payload"));
        assertThat(subscriber.getLastDuplicate(), is(nullValue()));
        assertThat(subscriber.isDuplicateEventSeen(), is(false));
        assertThat(subscriber.getPresentEventKeys(), is(Set.of("1", "2")));
        subscriber.onEvent(new GHSubscriberEvent(null, "origin", GHEvent.PUSH, "payload"));
        assertThat(subscriber.getLastDuplicate(), is(nullValue()));
        assertThat(subscriber.getPresentEventKeys(), is(Set.of("1", "2")));
        assertThat(subscriber.isDuplicateEventSeen(), is(false));

        // after a second
        fakeTicker.advance(Duration.ofSeconds(1));
        subscriber.onEvent(new GHSubscriberEvent("1", "origin", GHEvent.PUSH, "payload"));
        assertThat(subscriber.getLastDuplicate().eventGuid(), is("1"));
        assertThat(subscriber.getLastDuplicate().lastUpdated(), is(after1Sec));
        assertThat(subscriber.getPresentEventKeys(), is(Set.of("1", "2")));
        assertThat(subscriber.isDuplicateEventSeen(), is(true));

        // second occurrence for another event after 2 seconds
        fakeTicker.advance(Duration.ofSeconds(1));
        subscriber.onEvent(new GHSubscriberEvent("2", "origin", GHEvent.PUSH, "payload"));
        assertThat(subscriber.getLastDuplicate().eventGuid(), is("2"));
        assertThat(subscriber.getLastDuplicate().lastUpdated(), is(after2Sec));
        assertThat(subscriber.getPresentEventKeys(), is(Set.of("1", "2")));
        assertThat(subscriber.isDuplicateEventSeen(), is(true));

        // 24 hours has passed; note we already added 2 seconds/ so effectively 24h 2sec now.
        fakeTicker.advance(Duration.ofHours(24));
        assertThat(subscriber.isDuplicateEventSeen(), is(false));
    }

    @Test
    public void checkOldEntriesAreExpiredAfter10Minutes() {
        var subscriber = new GitHubDuplicateEventsMonitor.DuplicateEventsSubscriber();

        var now = Instant.parse("2025-02-05T03:00:00Z");
        FakeTicker fakeTicker = new FakeTicker(now);
        subscriber.setTicker(fakeTicker);

        // at present
        subscriber.onEvent(new GHSubscriberEvent("1", "origin", GHEvent.PUSH, "payload"));
        subscriber.onEvent(new GHSubscriberEvent("2", "origin", GHEvent.PUSH, "payload"));
        assertThat(subscriber.getPresentEventKeys(), is(Set.of("1", "2")));

        // after 2 minutes
        fakeTicker.advance(Duration.ofMinutes(2));
        subscriber.onEvent(new GHSubscriberEvent("3", "origin", GHEvent.PUSH, "payload"));
        subscriber.onEvent(new GHSubscriberEvent("4", "origin", GHEvent.PUSH, "payload"));
        assertThat(subscriber.getPresentEventKeys(), is(Set.of("1", "2", "3", "4")));
        assertThat(subscriber.getPresentEventKeys().size(), is(4));

        // 10 minutes 1 second later
        fakeTicker.advance(Duration.ofMinutes(8).plusSeconds(1));
        assertThat(subscriber.getPresentEventKeys(), is(Set.of("3", "4")));
        assertThat(subscriber.getPresentEventKeys().size(), is(2));
    }

    private static class FakeTicker implements Ticker {
        private final AtomicLong nanos = new AtomicLong();

        FakeTicker(Instant now) {
            nanos.set(now.toEpochMilli() * 1_000_000);
        }

        @Override
        public long read() {
            return nanos.get();
        }

        public void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }
}

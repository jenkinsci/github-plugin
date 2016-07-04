package org.jenkinsci.plugins.github.extension;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Job;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.util.misc.NullSafeFunction;
import org.jenkinsci.plugins.github.util.misc.NullSafePredicate;
import org.kohsuke.github.GHEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Extension point to subscribe events from GH, which plugin interested in.
 * This point should return true in {@link #isApplicable}
 * only if it can parse hooks with events contributed in {@link #events()}
 *
 * Each time this plugin wants to get events list from subscribers it asks for applicable status
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.12.0
 */
public abstract class GHEventsSubscriber implements ExtensionPoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(GHEventsSubscriber.class);

    /**
     * Should return true only if this subscriber interested in {@link #events()} set for this project
     * Don't call it directly, use {@link #isApplicableFor} static function
     *
     * @param project to check
     *
     * @return true to provide events to register and subscribe for this project
     */
    protected abstract boolean isApplicable(@Nullable Job<?, ?> project);

    /**
     * Should be not null. Should return only events which this extension can parse in {@link #onEvent(GHEvent, String)}
     * Don't call it directly, use {@link #extractEvents()} or {@link #isInterestedIn(GHEvent)} static functions
     *
     * @return immutable set of events this subscriber wants to register and then subscribe to.
     */
    protected abstract Set<GHEvent> events();

    /**
     * This method called when root action receives webhook from GH and this extension is interested in such
     * events (provided by {@link #events()} method). By default do nothing and can be overrided to implement any
     * parse logic
     * Don't call it directly, use {@link #processEvent(GHEvent, String, String)} static function
     *
     * @param event   gh-event (as of PUSH, ISSUE...). One of returned by {@link #events()} method. Never null.
     * @param payload payload of gh-event. Never blank. Can be parsed with help of GitHub#parseEventPayload
     * @param signature X-Hub-Signature header value, HMAC hex digest of payload from GitHub.
     */
    protected void onEvent(GHEvent event, String payload, String signature) {
        // do nothing by default
    }

    /**
     * @return All subscriber extensions
     */
    public static ExtensionList<GHEventsSubscriber> all() {
        return Jenkins.getInstance().getExtensionList(GHEventsSubscriber.class);
    }

    /**
     * Converts each subscriber to set of GHEvents
     *
     * @return converter to use in iterable manipulations
     */
    public static Function<GHEventsSubscriber, Set<GHEvent>> extractEvents() {
        return new NullSafeFunction<GHEventsSubscriber, Set<GHEvent>>() {
            @Override
            protected Set<GHEvent> applyNullSafe(@Nonnull GHEventsSubscriber subscriber) {
                return defaultIfNull(subscriber.events(), Collections.<GHEvent>emptySet());
            }
        };
    }

    /**
     * Helps to filter only GHEventsSubscribers that can return TRUE on given project
     *
     * @param project to check every GHEventsSubscriber for being applicable
     *
     * @return predicate to use in iterable filtering
     * @see #isApplicable
     */
    public static Predicate<GHEventsSubscriber> isApplicableFor(final Job<?, ?> project) {
        return new NullSafePredicate<GHEventsSubscriber>() {
            @Override
            protected boolean applyNullSafe(@Nonnull GHEventsSubscriber subscriber) {
                return subscriber.isApplicable(project);
            }
        };
    }

    /**
     * Predicate which returns true on apply if current subscriber is interested in event
     *
     * @param event should be one of {@link #events()} set to return true on apply
     *
     * @return predicate to match against {@link GHEventsSubscriber}
     */
    public static Predicate<GHEventsSubscriber> isInterestedIn(final GHEvent event) {
        return new NullSafePredicate<GHEventsSubscriber>() {
            @Override
            protected boolean applyNullSafe(@Nonnull GHEventsSubscriber subscriber) {
                return defaultIfNull(subscriber.events(), emptySet()).contains(event);
            }
        };
    }

    /**
     * Function which calls {@link #onEvent(GHEvent, String, String)} for every subscriber on apply
     *
     * @param event   from hook. Applied only with event from {@link #events()} set
     * @param payload string content of hook from GH. Never blank
     * @param signature HMAC
     *
     * @return function to process {@link GHEventsSubscriber} list. Returns null on apply.
     */
    public static Function<GHEventsSubscriber, Void> processEvent(final GHEvent event, final String payload, final String signature) {
        return new NullSafeFunction<GHEventsSubscriber, Void>() {
            @Override
            protected Void applyNullSafe(@Nonnull GHEventsSubscriber subscriber) {
                try {
                    subscriber.onEvent(event, payload, signature);
                } catch (Throwable t) {
                    LOGGER.error("Subscriber {} failed to process {} hook, skipping...",
                            subscriber.getClass().getName(), event, t);
                }
                return null;
            }
        };
    }
}

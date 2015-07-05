package org.jenkinsci.plugins.github.extension;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractProject;
import jenkins.model.Jenkins;
import org.kohsuke.github.GHEvent;

import java.util.Set;

/**
 * Extension point to contribute events from GH, which plugin interested in.
 * This point should return true in {@link #isApplicable(AbstractProject)}
 * only if it can parse hooks with events contributed in {@link #events()}
 *
 * Each time this plugin wants to get events list from contributors it asks for applicable status
 *
 * @author lanwen (Merkushev Kirill)
 * @since TODO
 */
public abstract class GHEventsSubscriber implements ExtensionPoint {

    /**
     * Should return true only if this subscriber interested in {@link #events()} set for this project
     *
     * @param project to check
     *
     * @return true to provide events to register and subscribe for this project
     */
    public abstract boolean isApplicable(AbstractProject<?, ?> project);

    /**
     * @return immutable set of events this subscriber wants to register and then subscribe to
     */
    public abstract Set<GHEvent> events();

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
        return new Function<GHEventsSubscriber, Set<GHEvent>>() {
            @Override
            public Set<GHEvent> apply(GHEventsSubscriber provider) {
                return provider.events();
            }
        };
    }

    /**
     * Helps to filter only GHEventsSubscribers that can return TRUE on given project
     *
     * @param project to check every GHEventsSubscriber for being applicable
     *
     * @return predicate to use in iterable filtering
     */
    public static Predicate<GHEventsSubscriber> isApplicableFor(final AbstractProject<?, ?> project) {
        return new Predicate<GHEventsSubscriber>() {
            @Override
            public boolean apply(GHEventsSubscriber provider) {
                return provider.isApplicable(project);
            }
        };
    }
}

package org.jenkinsci.plugins.github.webhook;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractProject;
import jenkins.model.Jenkins;
import org.kohsuke.github.GHEvent;

import java.util.Set;

/**
 * Extension point to contribute events plugin interested in.
 * This point should return true in {@link #isApplicable(AbstractProject)}
 * only if it can parse hooks with events contributed in {@link #events()}
 *
 * Each time this plugin wants to get events list from contributors it asks for applicable status
 *
 * @author lanwen (Merkushev Kirill)
 * @since TODO
 */
public abstract class GHEventsListener implements ExtensionPoint {

    /**
     * Should return true only if this listener interested in {@link #events()} set for this project
     *
     * @param project to check
     *
     * @return true to provide events to register and listen for this project
     */
    public abstract boolean isApplicable(AbstractProject<?, ?> project);

    /**
     * @return immutable set of events this listener wants to register and then listen to
     */
    public abstract Set<GHEvent> events();

    /**
     * @return All listener extensions
     */
    public static ExtensionList<GHEventsListener> all() {
        return Jenkins.getInstance().getExtensionList(GHEventsListener.class);
    }

    /**
     * Converts every provider to set of GHEvents
     *
     * @return converter to use in iterable manipulations
     */
    public static Function<GHEventsListener, Set<GHEvent>> extractEvents() {
        return new Function<GHEventsListener, Set<GHEvent>>() {
            @Override
            public Set<GHEvent> apply(GHEventsListener provider) {
                return provider.events();
            }
        };
    }

    /**
     * Helps to filter only GHEventsListeners that can return TRUE on given project
     *
     * @param project to check every GHEventsListener for being applicable
     *
     * @return predicate to use in iterable filtering
     */
    public static Predicate<GHEventsListener> isApplicableFor(final AbstractProject<?, ?> project) {
        return new Predicate<GHEventsListener>() {
            @Override
            public boolean apply(GHEventsListener provider) {
                return provider.isApplicable(project);
            }
        };
    }
}

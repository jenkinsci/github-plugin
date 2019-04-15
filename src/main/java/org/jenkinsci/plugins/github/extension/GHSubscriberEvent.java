package org.jenkinsci.plugins.github.extension;

import jenkins.scm.api.SCMEvent;
import org.kohsuke.github.GHEvent;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * An event for a {@link GHEventsSubscriber}.
 *
 * @since 1.26.0
 */
public class GHSubscriberEvent extends SCMEvent<String> {
    /**
     * The type of event.
     */
    private final GHEvent ghEvent;

    /**
     * Constructs a new {@link GHSubscriberEvent}.
     *
     * @param origin  the origin (see {@link SCMEvent#originOf(javax.servlet.http.HttpServletRequest)}) or {@code null}.
     * @param ghEvent the type of event received from GitHub.
     * @param payload the event payload.
     */
    public GHSubscriberEvent(@CheckForNull String origin, @Nonnull GHEvent ghEvent, @Nonnull String payload) {
        super(Type.UPDATED, payload, origin);
        this.ghEvent = ghEvent;
    }

    /**
     * Gets the type of event received.
     *
     * @return the type of event received.
     */
    public GHEvent getGHEvent() {
        return ghEvent;
    }

}

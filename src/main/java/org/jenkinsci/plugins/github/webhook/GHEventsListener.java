package org.jenkinsci.plugins.github.webhook;

import com.cloudbees.jenkins.GitHubPushTrigger;
import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractProject;
import jenkins.model.Jenkins;
import org.kohsuke.github.GHEvent;

import java.util.Set;

import static com.google.common.collect.Sets.immutableEnumSet;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.withTrigger;
import static org.kohsuke.github.GHEvent.PUSH;

/**
 * Extension point to contribute events plugin interested in.
 * This point should return true in {@link #isApplicable(AbstractProject)}
 * only if it can parse hooks with events contributed in {@link #events()}
 *
 * Each time this plugin wants to get events list from contributors it asks for applicable status
 *
 * @author lanwen (Merkushev Kirill)
 *         Date: 16.06.15
 */
public abstract class GHEventsListener implements ExtensionPoint {

    public abstract boolean isApplicable(AbstractProject<?, ?> project);

    public abstract Set<GHEvent> events();

    @Beta
    public void processEvent(GHEvent event, String payload) {
        // TODO can be changed
    }

    public static ExtensionList<GHEventsListener> all() {
        return Jenkins.getInstance().getExtensionList(GHEventsListener.class);
    }

    public static Function<GHEventsListener, Set<GHEvent>> extractEvents() {
        return new Function<GHEventsListener, Set<GHEvent>>() {
            @Override
            public Set<GHEvent> apply(GHEventsListener provider) {
                return provider.events();
            }
        };
    }

    public static Predicate<GHEventsListener> isApplicableFor(final AbstractProject<?, ?> project) {
        return new Predicate<GHEventsListener>() {
            @Override
            public boolean apply(GHEventsListener provider) {
                return provider.isApplicable(project);
            }
        };
    }

    @Extension
    @SuppressWarnings("unused")
    public static class DefaultPushGHEventListener extends GHEventsListener {
        @Override
        public boolean isApplicable(AbstractProject<?, ?> project) {
            return withTrigger(GitHubPushTrigger.class).apply(project);
        }

        @Override
        public Set<GHEvent> events() {
            return immutableEnumSet(PUSH);
        }
    }

}

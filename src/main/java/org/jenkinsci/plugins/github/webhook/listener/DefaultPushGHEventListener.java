package org.jenkinsci.plugins.github.webhook.listener;

import com.cloudbees.jenkins.GitHubPushTrigger;
import hudson.Extension;
import hudson.model.AbstractProject;
import org.jenkinsci.plugins.github.webhook.GHEventsListener;
import org.kohsuke.github.GHEvent;

import java.util.Set;

import static com.google.common.collect.Sets.immutableEnumSet;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.withTrigger;
import static org.kohsuke.github.GHEvent.PUSH;

/**
 * By default this plugin interested in push events only when job uses {@link GitHubPushTrigger}
 *
 * @author lanwen (Merkushev Kirill)
 * @since TODO
 */
@Extension
@SuppressWarnings("unused")
public class DefaultPushGHEventListener extends GHEventsListener {
    /**
     * This listener is applicable only for job with GHPush trigger
     *
     * @param project to check for trigger
     *
     * @return true if project has {@link GitHubPushTrigger}
     */
    @Override
    public boolean isApplicable(AbstractProject<?, ?> project) {
        return withTrigger(GitHubPushTrigger.class).apply(project);
    }

    /**
     * @return set with only push event
     */
    @Override
    public Set<GHEvent> events() {
        return immutableEnumSet(PUSH);
    }
}

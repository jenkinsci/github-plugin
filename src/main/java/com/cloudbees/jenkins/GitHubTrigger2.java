package com.cloudbees.jenkins;

import hudson.triggers.Trigger;

/**
 * Optional interface that can be implemented by {@link Trigger} that watches out for a change in GitHub
 * and triggers a build.
 *
 * @author aaronwalker
 */
public interface GitHubTrigger2 extends GitHubTrigger {

    /**
     * Callback to notify when a change in GitHub triggeres a build.
     * @param event the event details.
     */
    void onPost(GitHubTriggerEvent event);
}

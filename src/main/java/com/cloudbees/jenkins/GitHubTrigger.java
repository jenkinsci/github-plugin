package com.cloudbees.jenkins;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.triggers.Trigger;

import java.util.Collection;
import java.util.Set;

import jenkins.model.ParameterizedJobMixIn;

/**
 * Optional interface that can be implemented by {@link Trigger} that watches out for a change in GitHub
 * and triggers a build.
 *
 * @author aaronwalker
 */
public interface GitHubTrigger {

    @Deprecated
    public void onPost();

    // TODO: document me
    public void onPost(String triggeredByUser);
    /**
     * Obtains the list of the repositories that this trigger is looking at.
     *
     * If the implementation of this class maintain its own list of GitHub repositories, it should
     * continue to implement this method for backward compatibility, and it gets picked up by
     * {@link GitHubRepositoryNameContributor#parseAssociatedNames(AbstractProject)}.
     *
     * <p>
     * Alternatively, if the implementation doesn't worry about the backward compatibility, it can
     * implement this method to return an empty collection, then just implement {@link GitHubRepositoryNameContributor}.
     *
     * @deprecated
     *      Call {@link GitHubRepositoryNameContributor#parseAssociatedNames(AbstractProject)} instead.
     */
    public Set<GitHubRepositoryName> getGitHubRepositories();

    /**
     * Contributes {@link GitHubRepositoryName} from {@link GitHubTrigger#getGitHubRepositories()}
     * for backward compatibility
     */
    @Extension
    public static class GitHubRepositoryNameContributorImpl extends GitHubRepositoryNameContributor {
        @Override
        public void parseAssociatedNames(Job<?, ?> job, Collection<GitHubRepositoryName> result) {
            if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
                ParameterizedJobMixIn.ParameterizedJob p = (ParameterizedJobMixIn.ParameterizedJob) job;
                for (GitHubTrigger ght : Util.filter(p.getTriggers().values(),GitHubTrigger.class)) {
                    result.addAll(ght.getGitHubRepositories());
                }
            }
        }
    }
}

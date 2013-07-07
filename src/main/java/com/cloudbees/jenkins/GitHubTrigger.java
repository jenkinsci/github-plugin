package com.cloudbees.jenkins;

import hudson.model.AbstractProject;

import java.util.Set;

/**
 * Used to trigger builds that use github repos
 *
 * @author aaronwalker
 */
public interface GitHubTrigger  {

    @Deprecated
    public void onPost();
    public void onPost(String triggeredByUser);
    /**
     * @deprecated
     *      Use {@link GitHubRepositoryName#from(AbstractProject)}
     */
    public Set<GitHubRepositoryName> getGitHubRepositories();
}

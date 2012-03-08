package com.cloudbees.jenkins;

import hudson.model.AbstractProject;
import hudson.triggers.Trigger;

import java.util.Set;

/**
 * Used to trigger builds that use github repos
 *
 * @author aaronwalker
 */
public interface GitHubTrigger  {

    public void onPost();
    public Set<GitHubRepositoryName> getGitHubRepositories();
}

package com.cloudbees.jenkins;

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
    public Set<GitHubRepositoryName> getGitHubRepositories();
}

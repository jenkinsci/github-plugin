package com.cloudbees.jenkins;

import hudson.plugins.git.BranchSpec;
import junit.framework.Assert;

import org.junit.Test;

public class GitHubBranchTest {

    @Test
    public void testBranchMatch() throws Exception, InterruptedException {
        GitHubRepositoryName repo = GitHubRepositoryName.create("github-repo");

        BranchSpec scmBranch = new BranchSpec("*/github-branch");
        GitHubBranch branch = GitHubBranch.create(scmBranch);

        Assert.assertTrue(branch.matches(repo, "refs/heads/github-branch"));
        Assert.assertTrue(branch.matches(repo, "github-branch"));
    }

    @Test
    public void testBranchDoesNotMatch() throws Exception, InterruptedException {
        GitHubRepositoryName repo = GitHubRepositoryName.create("github-repo");

        BranchSpec scmBranch = new BranchSpec("*/github-branch");
        GitHubBranch branch = GitHubBranch.create(scmBranch);

        Assert.assertFalse(branch.matches(repo, "refs/heads/github-branch-2"));
        Assert.assertFalse(branch.matches(repo, "github-branch-2"));
    }

    @Test
    public void testRepoDoesNotMatch() throws Exception, InterruptedException {
        GitHubRepositoryName repo = GitHubRepositoryName.create("github-repo");

        BranchSpec scmBranch = new BranchSpec("github-repo-2/github-branch");
        GitHubBranch branch = GitHubBranch.create(scmBranch);

        Assert.assertFalse(branch.matches(repo, "refs/heads/github-branch"));
        Assert.assertFalse(branch.matches(repo, "github-branch"));
    }
}

package com.cloudbees.jenkins;

import hudson.plugins.git.BranchSpec;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Identifies a GitHub branch and hides the SCM branch from its clients.
 */
public class GitHubBranch {

    private static final String GITHUB_BRANCH_PREFIX = "refs/heads";
    private static final int GITHUB_BRANCH_PREFIX_LENGTH = 10;

    private final BranchSpec branch;

    private GitHubBranch(final BranchSpec branch) {
        this.branch = branch;
    }

    public boolean matches(GitHubRepositoryName repository, String ref) {
        // ref (github) -> refs/heads/master
        // branch (git SCM) -> REPO/remote/branch
        // matching the meaningful part of the github branch name with
        // the configured SCM branch expression
        if (ref != null && !ref.equals("")) {
            String branchExpression = "*";
            if (repository != null && repository.repositoryName != null) {
                branchExpression = repository.repositoryName;
            }
            if (ref.startsWith(GITHUB_BRANCH_PREFIX)) {
                branchExpression += ref.substring(GITHUB_BRANCH_PREFIX_LENGTH);
            } else {
                branchExpression += "/" + ref;
            }
            LOGGER.log(Level.FINE, "Does SCM branch " + branch.getName()
                    + " match GitHub branch " + branchExpression + "?");
            return branch.matches(branchExpression);
        }
        return false;
    }

    public static GitHubBranch create(BranchSpec branch) {
        if (isValidGitHubBranch(branch)) {
            return new GitHubBranch(branch);
        } else {
            return null;
        }
    }

    // TODO: implement logic to validate git SCM branches.
    private static boolean isValidGitHubBranch(BranchSpec branch) {
        return true;
    }

    private static final Logger LOGGER = Logger.getLogger(GitHubBranch.class
            .getName());
}

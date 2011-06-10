package com.cloudbees.jenkins;

import hudson.model.Cause;

/**
 * UI object that says a build is started by GitHub post-commit hook.
 *
 * @author Kohsuke Kawaguchi
 */
public class GitHubPushCause extends Cause {
    @Override
    public String getShortDescription() {
        return "Started by GitHub push by ";
    }
}

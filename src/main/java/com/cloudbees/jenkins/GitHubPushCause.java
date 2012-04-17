package com.cloudbees.jenkins;

import hudson.model.Cause;
import hudson.triggers.SCMTrigger.SCMTriggerCause;

/**
 * UI object that says a build is started by GitHub post-commit hook.
 *
 * @author Kohsuke Kawaguchi
 */
public class GitHubPushCause extends SCMTriggerCause {
    @Override
    public String getShortDescription() {
        return "Started by GitHub push by ";
    }
}

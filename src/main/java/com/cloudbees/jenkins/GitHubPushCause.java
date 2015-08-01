package com.cloudbees.jenkins;

import hudson.triggers.SCMTrigger.SCMTriggerCause;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * UI object that says a build is started by GitHub post-commit hook.
 *
 * @author Kohsuke Kawaguchi
 */
public class GitHubPushCause extends SCMTriggerCause {
    /**
     * The name of the user who pushed to GitHub.
     */
    private String pushedBy;

    public GitHubPushCause(String pusher) {
        this("", pusher);
    }

    public GitHubPushCause(String pollingLog, String pusher) {
        super(pollingLog);
        pushedBy = pusher;
    }

    public GitHubPushCause(File pollingLog, String pusher) throws IOException {
        super(pollingLog);
        pushedBy = pusher;
    }

    @Override
    public String getShortDescription() {
        String pusher = pushedBy != null ? pushedBy : "";
        return "Started by GitHub push by " + pusher;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.pushedBy);
    }

    @Override
    public boolean equals(Object obj) {
        if (!getClass().isInstance(obj)) {
            return false;
        }
        final GitHubPushCause other = (GitHubPushCause) obj;
        if ((this.pushedBy == null) ? (other.pushedBy != null) : !this.pushedBy.equals(other.pushedBy)) {
            return false;
        }
        return true;
    }
}

package com.cloudbees.jenkins;

import hudson.triggers.SCMTrigger.SCMTriggerCause;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

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
        return format("Started by GitHub push by %s", trimToEmpty(pushedBy));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GitHubPushCause
                && Objects.equals(this.pushedBy, ((GitHubPushCause) o).pushedBy)
                && super.equals(o);
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 89 * hash + Objects.hash(this.pushedBy);
        return hash;
    }
}


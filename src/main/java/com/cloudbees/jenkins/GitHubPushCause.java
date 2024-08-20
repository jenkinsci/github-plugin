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
    /**
     * The target ref of the triggering GitHub push.
     */
    private String ref;

    public GitHubPushCause(String pusher, String ref) {
        this("", pusher, ref);
    }

    public GitHubPushCause(String pollingLog, String pusher, String ref) {
        super(pollingLog);
        this.pushedBy = pusher;
        this.ref = ref;
    }

    public GitHubPushCause(File pollingLog, String pusher, String ref) throws IOException {
        super(pollingLog);
        this.pushedBy = pusher;
        this.ref = ref;
    }

    @Override
    public String getShortDescription() {
        return format("Started by GitHub push to %s by %s", trimToEmpty(ref), trimToEmpty(pushedBy));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GitHubPushCause
                && Objects.equals(this.pushedBy, ((GitHubPushCause) o).pushedBy)
                && Objects.equals(this.ref, ((GitHubPushCause) o).ref)
                && super.equals(o);
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 89 * hash + Objects.hash(this.pushedBy);
        hash = 89 * hash + Objects.hash(this.ref);
        return hash;
    }
}

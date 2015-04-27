package com.cloudbees.jenkins;

import hudson.triggers.SCMTrigger.SCMTriggerCause;

import java.io.File;
import java.io.IOException;

/**
 * UI object that says a build is started by GitHub post-commit hook.
 *
 * @author Marcus Chan
 */
public class GitHubCause extends SCMTriggerCause {

    private String eventType;

    private String name;

    public GitHubCause(String eventType, String name) {
        this("", eventType, name);
    }

    public GitHubCause(String pollingLog, String eventType, String name) {
        super(pollingLog);
        this.eventType = eventType;
        this.name = name;
    }

    public GitHubCause(File pollingLog, String eventType, String name) throws IOException {
        super(pollingLog);
        this.eventType = eventType;
        this.name = name;
    }

    @Override
    public String getShortDescription() {
        String event = eventType != null ? eventType : "";
        String author = name != null ? name : "";
        return "Started by GitHub " + event + " by " + author;
    }
}

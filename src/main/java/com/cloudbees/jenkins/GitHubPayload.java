package com.cloudbees.jenkins;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Inject the payload received by GitHub into the build through $GITHUB_PAYLOAD so it can be processed
 * @since January 28, 2016
 * @version 1.17.1
 */
public class GitHubPayload extends InvisibleAction implements EnvironmentContributingAction {
    private final String payload;

    public GitHubPayload(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> abstractBuild, EnvVars envVars) {
        LOGGER.log(Level.FINEST, "Injecting GITHUB_PAYLOAD: {0}", payload);
        envVars.put("GITHUB_PAYLOAD", payload);
    }

    private static final Logger LOGGER = Logger.getLogger(GitHubPayload.class.getName());
}

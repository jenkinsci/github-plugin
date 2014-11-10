package com.cloudbees.jenkins;

import com.cloudbees.jenkins.github.AccessTokenCredential;
import com.cloudbees.jenkins.github.GitHubServerConfig;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.security.ACL;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.cloudbees.plugins.credentials.CredentialsScope.GLOBAL;

/**
 * Credential to access GitHub.
 *
 * @author Kohsuke Kawaguchi
 */
@Deprecated
public class Credential extends GitHubServerConfig {

    public transient String username;
    public transient String oauthAccessToken;

    private Credential(String apiUrl, String credentialId) {
        super(apiUrl, credentialId);
    }

    // Migrate legacy data
    private Object readResolve() {

        if (credentialId != null) return this;

        if (apiUrl == null) apiUrl = "https://api.github.com"; // org.kohsuke.github.GitHub.GITHUB_URL is private

        // search for existing credentials
        List<AccessTokenCredential> candidates = CredentialsProvider.lookupCredentials(AccessTokenCredential.class, Jenkins.getInstance(), ACL.SYSTEM,
                URIRequirementBuilder.fromUri(apiUrl).build());
        for (AccessTokenCredential candidate : candidates) {
            if (candidate.getUsername().equals(username)) {
                return new GitHubServerConfig(apiUrl, candidate.getId());
            }
        }

        CredentialsStore store = CredentialsProvider.lookupStores(Jenkins.getInstance()).iterator().next();
        AccessTokenCredential accessToken = new AccessTokenCredential(GLOBAL, null, apiUrl, apiUrl, username, oauthAccessToken);
        try {
            store.addCredentials(Domain.global(), accessToken);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "failed to migrate GitHub OAuth token to credentials store", e);
        }

        return new GitHubServerConfig(apiUrl, accessToken.getId());
    }

    private static final Logger LOGGER = Logger.getLogger(Credential.class.getName());
}

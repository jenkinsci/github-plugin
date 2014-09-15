package com.cloudbees.jenkins.github;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collections;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GitHubServerConfig extends AbstractDescribableImpl<GitHubServerConfig> {

    public String apiUrl;
    protected final String credentialId;

    @DataBoundConstructor
    public GitHubServerConfig(String apiUrl, String credentialId) {
        this.apiUrl = apiUrl;
        this.credentialId = credentialId;
    }

    public GitHub login() throws IOException {

        AccessTokenCredential accessToken = (AccessTokenCredential) CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(AccessTokenCredential.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.EMPTY_LIST),
                CredentialsMatchers.withId(credentialId));

        if (Util.fixEmpty(apiUrl) != null) {
            return GitHub.connectToEnterprise(apiUrl,accessToken.getToken().getPlainText());
        }
        return GitHub.connect(accessToken.getUsername(),accessToken.getToken().getPlainText());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GitHubServerConfig> {
        @Override
        public String getDisplayName() {
            return ""; // unused
        }

    }
}

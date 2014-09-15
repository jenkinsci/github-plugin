package com.cloudbees.jenkins.github;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.Util;
import hudson.model.AutoCompletionCandidates;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.kohsuke.github.GHAuthorization;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class AccessTokenCredential extends BaseStandardCredentials implements StandardUsernameCredentials {

    private final String githubServer;

    private final String username;

    private final Secret token;

    @DataBoundConstructor
    public AccessTokenCredential(CredentialsScope scope,
                                 String id, String description,
                                 String githubServer, String username, String token) {
        super(scope, id, description);
        this.githubServer = githubServer;
        this.username = Util.fixNull(username);
        this.token = Secret.fromString(token);
    }

    public String getUsername() {
        return username;
    }

    public Secret getToken() {
        return token;
    }

    @Extension
    public static class DescriptorImpl extends CredentialsDescriptor {

        @Override
        public String getDisplayName() {
            return "Github Access Token";
        }


        public FormValidation doCreateToken(@QueryParameter("githubServer") String githubServer,
                                            @QueryParameter("username") String username,
                                            @QueryParameter("password") String password,
                                            @QueryParameter("scope") String scopes) {


            String[] sc = scopes.split("\\s*,[,\\s]*"); // GHAuthorization.REPO_STATUS, GHAuthorization.REPO

            try {
                GitHub gh = GitHub.connectToEnterprise(githubServer, username, password);
                GHAuthorization token = gh.createToken(Arrays.asList(sc),
                        "Jenkins GitHub Plugin", Jenkins.getInstance().getRootUrl());
                return FormValidation.ok("Access token created: " + token.getToken());
            } catch (IOException ex) {
                return FormValidation.error("GitHub API token couldn't be created: " + ex.getMessage());
            }
        }

        // TODO build from GHAuthorization.*, but require https://github.com/kohsuke/github-api/pull/128
        final List<String> scopes = Arrays.asList("user", "user:email", "user:follow",
                "public_repo", "repo", "repo_deployment", "repo:status", "delete_repo",
                "notifications", "gist",
                "read:repo_hook", "write:repo_hook", "admin:repo_hook",
                "read:org", "write:org", "admin:org",
                "read:public_key", "write:public_key", "admin :public_key");

        public AutoCompletionCandidates doAutoCompleteScope(@QueryParameter String value) {
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            for (String scope : scopes) {
                if (scope.startsWith(value)) candidates.add(scope);
            }
            return candidates;
        }


        public FormValidation doValidate(@QueryParameter String apiUrl, @QueryParameter String username, @QueryParameter String oauthAccessToken) throws IOException {
            GitHub gitHub;
            if (Util.fixEmpty(apiUrl) != null) {
                gitHub = GitHub.connectToEnterprise(apiUrl,oauthAccessToken);
            } else {
                gitHub = GitHub.connect(username,oauthAccessToken);
            }

            if (gitHub.isCredentialValid())
                return FormValidation.ok("Verified");
            else
                return FormValidation.error("Failed to validate the account");
        }
    }
}

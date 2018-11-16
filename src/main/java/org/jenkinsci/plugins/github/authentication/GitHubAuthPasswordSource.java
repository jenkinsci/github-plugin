package org.jenkinsci.plugins.github.authentication;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.authentication.tokens.api.AuthenticationTokenException;
import jenkins.authentication.tokens.api.AuthenticationTokenSource;

/**
 * Converts {@link UsernamePasswordCredentials} to {@link GitHubAuthPassword} authentication.
 */
@Extension
public class GitHubAuthPasswordSource
        extends AuthenticationTokenSource<GitHubAuthPassword, UsernamePasswordCredentials> {
    /**
     * Constructor.
     */
    public GitHubAuthPasswordSource() {
        super(GitHubAuthPassword.class, UsernamePasswordCredentials.class);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public GitHubAuthPassword convert(@NonNull UsernamePasswordCredentials credential)
            throws AuthenticationTokenException {
        return new GitHubAuthPassword(credential.getUsername(), credential.getPassword().getPlainText());
    }
}

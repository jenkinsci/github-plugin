package org.jenkinsci.plugins.github.authentication;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import jenkins.authentication.tokens.api.AuthenticationTokenException;
import jenkins.authentication.tokens.api.AuthenticationTokenSource;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;

/**
 * Converts {@link StringCredentials} to {@link GitHubAuthPassword} authentication.
 */
// TODO migrate StringCredentials to GitHubOAuthTokenCredentuals.
@Extension
public class GitHubAuthOAuthTokenSource
        extends AuthenticationTokenSource<GitHubAuthOAuthToken, StringCredentials> {
    /**
     * Constructor.
     */
    public GitHubAuthOAuthTokenSource() {
        super(GitHubAuthOAuthToken.class, StringCredentials.class);
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public GitHubAuthOAuthToken convert(@NonNull StringCredentials credential)
            throws AuthenticationTokenException {
        return new GitHubAuthOAuthToken(credential.getSecret().getPlainText());
    }
}

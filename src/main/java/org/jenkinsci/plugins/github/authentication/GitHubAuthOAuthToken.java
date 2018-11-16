package org.jenkinsci.plugins.github.authentication;

import org.kohsuke.github.GitHubBuilder;

/**
 * Represents OAuth token authentication for {@link GitHubBuilder}.
 */
public class GitHubAuthOAuthToken extends GitHubAuth {

    /**
     * Standardize serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The username (optional).
     */
    private final String username;
    /**
     * The OAuth token.
     */
    private final String token;

    /**
     * Constructor.
     *
     * @param username the optional username.
     * @param token the oauth token.
     */
    public GitHubAuthOAuthToken(String username, String token) {
        this.username = username;
        this.token = token;
    }

    /**
     * Constructor.
     *
     * @param token the oauth token.
     */
    public GitHubAuthOAuthToken(String token) {
        this(null, token);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addAuthentication(GitHubBuilder builder) {
        if (username == null) {
            builder.withOAuthToken(token);
        } else {
            builder.withOAuthToken(token, username);
        }
    }
}

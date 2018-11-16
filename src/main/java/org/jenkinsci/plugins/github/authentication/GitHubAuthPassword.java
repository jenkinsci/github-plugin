package org.jenkinsci.plugins.github.authentication;

import org.kohsuke.github.GitHubBuilder;

/**
 * Represents username password authentication for {@link GitHubBuilder}.
 */
public class GitHubAuthPassword extends GitHubAuth {

    /**
     * Standardize serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The username.
     */
    private final String username;
    /**
     * The password.
     */
    private final String password;

    /**
     * Constructor.
     * @param username the username.
     * @param password the password.
     */
    public GitHubAuthPassword(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addAuthentication(GitHubBuilder builder) {
        builder.withPassword(username, password);
    }
}

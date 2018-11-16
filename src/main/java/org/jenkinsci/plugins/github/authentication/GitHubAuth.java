package org.jenkinsci.plugins.github.authentication;

import com.cloudbees.plugins.credentials.Credentials;
import java.io.Serializable;
import jenkins.authentication.tokens.api.AuthenticationTokenContext;
import jenkins.authentication.tokens.api.AuthenticationTokens;
import org.kohsuke.github.GitHubBuilder;

/**
 * Abstraction of the various authentication mechanisms. Use {@link AuthenticationTokens#convert(Class, Credentials)} to
 * get an implementation from {@link Credentials}.
 */
public abstract class GitHubAuth implements Serializable {

    /**
     * The key for GitHub API URL as rerported in an {@link AuthenticationTokenContext}
     */
    public static final String API_URL = "github.api.uri";
    /**
     * Standardize serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Ádds the authentication to the supplied {@link GitHubBuilder}.
     *
     * @param builder the {@link GitHubBuilder} to authenticate.
     * @return the supplied builder for method chaining.
     */
    public final GitHubBuilder authenticate(GitHubBuilder builder) {
        addAuthentication(builder);
        return builder;
    }

    /**
     * Ádds the authentication to the supplied {@link GitHubBuilder}.
     *
     * @param builder the {@link GitHubBuilder} to authenticate.
     */
    protected abstract void addAuthentication(GitHubBuilder builder);
}

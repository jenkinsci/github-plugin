package org.jenkinsci.plugins.github.deprecated;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Credential to access GitHub.
 * Used only for migration.
 *
 * @author Kohsuke Kawaguchi
 * @deprecated Please use {@link org.jenkinsci.plugins.github.config.GitHubServerConfig} instead
 */
@Deprecated
public class Credential {
    private final transient String username;
    private final transient String apiUrl;
    private final transient String oauthAccessToken;

    @DataBoundConstructor
    public Credential(String username, String apiUrl, String oauthAccessToken) {
        this.username = username;
        this.apiUrl = apiUrl;
        this.oauthAccessToken = oauthAccessToken;
    }

    public String getUsername() {
        return username;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getOauthAccessToken() {
        return oauthAccessToken;
    }
}

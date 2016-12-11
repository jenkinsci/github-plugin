package com.cloudbees.jenkins;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import java.io.IOException;

import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * Credential to access GitHub.
 * Used only for migration.
 *
 * @author Kohsuke Kawaguchi
 * @see org.jenkinsci.plugins.github.config.GitHubPluginConfig
 * @see GitHubServerConfig
 * @deprecated since 1.13.0 plugin uses credentials-plugin to manage tokens. All configuration moved to
 * {@link org.jenkinsci.plugins.github.config.GitHubPluginConfig} which can be fetched via
 * {@link GitHubPlugin#configuration()}. You can fetch corresponding config with creds by
 * {@link org.jenkinsci.plugins.github.config.GitHubPluginConfig#findGithubConfig(Predicate)} which returns
 * iterable over authorized nonnull {@link GitHub}s matched your predicate
 */
@Deprecated
public class Credential {
    @SuppressWarnings("visibilitymodifier")
    public final transient String username;
    @SuppressWarnings("visibilitymodifier")
    public final transient String apiUrl;
    @SuppressWarnings("visibilitymodifier")
    public final transient String oauthAccessToken;

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

    /**
     * @return authorized first {@link GitHub} from global config or null if no any
     * @throws IOException never thrown, but in signature for backward compatibility
     * @deprecated see class javadoc. Now any instance return same GH. Please use new api to fetch another
     */
    @CheckForNull
    @Deprecated
    public GitHub login() throws IOException {
        return from(GitHubPlugin.configuration().findGithubConfig(Predicates.<GitHubServerConfig>alwaysTrue()))
                .first().orNull();
    }
}

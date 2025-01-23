package org.jenkinsci.plugins.github.config;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.GITHUB_URL;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.allowedToManageHooks;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.isUrlCustom;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.withHost;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubServerConfigTest {

    public static final String CUSTOM_GH_SERVER = "http://some.com";
    public static final String DEFAULT_GH_API_HOST = "api.github.com";

    @Test
    void shouldMatchAllowedConfig() throws Exception {
        assertThat(allowedToManageHooks().apply(new GitHubServerConfig("")), is(true));
    }

    @Test
    void shouldNotMatchNotAllowedConfig() throws Exception {
        GitHubServerConfig input = new GitHubServerConfig("");
        input.setManageHooks(false);
        assertThat(allowedToManageHooks().apply(input), is(false));
    }

    @Test
    void shouldMatchNonEqualToGHUrl() throws Exception {
          assertThat(isUrlCustom(CUSTOM_GH_SERVER), is(true));
    }

    @Test
    void shouldNotMatchEmptyUrl() throws Exception {
          assertThat(isUrlCustom(""), is(false));
    }

    @Test
    void shouldNotMatchNullUrl() throws Exception {
          assertThat(isUrlCustom(null), is(false));
    }

    @Test
    void shouldNotMatchDefaultUrl() throws Exception {
          assertThat(isUrlCustom(GITHUB_URL), is(false));
    }

    @Test
    void shouldMatchDefaultConfigWithGHDefaultHost() throws Exception {
        assertThat(withHost(DEFAULT_GH_API_HOST).apply(new GitHubServerConfig("")), is(true));
    }

    @Test
    void shouldNotMatchNonDefaultConfigWithGHDefaultHost() throws Exception {
        GitHubServerConfig input = new GitHubServerConfig("");
        input.setApiUrl(CUSTOM_GH_SERVER);
        assertThat(withHost(DEFAULT_GH_API_HOST).apply(input), is(false));
    }

    @Test
    void shouldNotMatchDefaultConfigWithNonDefaultHost() throws Exception {
        assertThat(withHost(URI.create(CUSTOM_GH_SERVER).getHost()).apply(new GitHubServerConfig("")), is(false));
    }

    @Test
    void shouldGuessNameIfNotProvided() throws Exception {
        GitHubServerConfig input = new GitHubServerConfig("");
        input.setApiUrl(CUSTOM_GH_SERVER);
        assertThat(input.getName(), is(nullValue()));
        assertThat(input.getDisplayName(), is("some (http://some.com)"));
    }

    @Test
    void shouldPickCorrectNamesForGitHub() throws Exception {
        GitHubServerConfig input = new GitHubServerConfig("");
        assertThat(input.getName(), is(nullValue()));
        assertThat(input.getDisplayName(), is("GitHub (https://github.com)"));
    }

    @Test
    void shouldUseNameIfProvided() throws Exception {
        GitHubServerConfig input = new GitHubServerConfig("");
        input.setApiUrl(CUSTOM_GH_SERVER);
        input.setName("Test Example");
        assertThat(input.getName(), is("Test Example"));
        assertThat(input.getDisplayName(), is("Test Example (http://some.com)"));
    }
}

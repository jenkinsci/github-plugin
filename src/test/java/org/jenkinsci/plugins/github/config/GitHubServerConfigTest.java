package org.jenkinsci.plugins.github.config;

import org.junit.Test;

import java.net.URI;

import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.GITHUB_URL;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.allowedToManageHooks;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.isUrlCustom;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.withHost;
import static org.junit.Assert.assertThat;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubServerConfigTest {

    public static final String CUSTOM_GH_SERVER = "http://some.com";
    public static final String DEFAULT_GH_API_HOST = "api.github.com";

    @Test
    public void shouldMatchAllowedConfig() throws Exception {
        assertThat(allowedToManageHooks().apply(new GitHubServerConfig("")), is(true));
    }

    @Test
    public void shouldNotMatchNotAllowedConfig() throws Exception {
        GitHubServerConfig input = new GitHubServerConfig("");
        input.setManageHooks(false);
        assertThat(allowedToManageHooks().apply(input), is(false));
    }

    @Test
    public void shouldMatchNonEqualToGHUrl() throws Exception {
          assertThat(isUrlCustom(CUSTOM_GH_SERVER), is(true));
    }

    @Test
    public void shouldNotMatchEmptyUrl() throws Exception {
          assertThat(isUrlCustom(""), is(false));
    }

    @Test
    public void shouldNotMatchNullUrl() throws Exception {
          assertThat(isUrlCustom(null), is(false));
    }

    @Test
    public void shouldNotMatchDefaultUrl() throws Exception {
          assertThat(isUrlCustom(GITHUB_URL), is(false));
    }

    @Test
    public void shouldMatchDefaultConfigWithGHDefaultHost() throws Exception {
        assertThat(withHost(DEFAULT_GH_API_HOST).apply(new GitHubServerConfig("")), is(true));
    }

    @Test
    public void shouldNotMatchNonDefaultConfigWithGHDefaultHost() throws Exception {
        GitHubServerConfig input = new GitHubServerConfig("");
        input.setCustom(true);
        input.setApiUrl(CUSTOM_GH_SERVER);
        assertThat(withHost(DEFAULT_GH_API_HOST).apply(input), is(false));
    }

    @Test
    public void shouldNotMatchDefaultConfigWithNonDefaultHost() throws Exception {
        assertThat(withHost(URI.create(CUSTOM_GH_SERVER).getHost()).apply(new GitHubServerConfig("")), is(false));
    }
}

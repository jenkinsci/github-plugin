package org.jenkinsci.plugins.github.config;

import org.jenkinsci.plugins.github.GitHubPlugin;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubPluginConfigTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void shouldNotManageHooksOnEmptyCreds() throws Exception {
        assertThat(GitHubPlugin.configuration().isManageHooks(), is(false));
    }

    @Test
    public void shouldManageHooksOnManagedConfig() throws Exception {
        GitHubPlugin.configuration().getConfigs().add(new GitHubServerConfig(""));
        assertThat(GitHubPlugin.configuration().isManageHooks(), is(true));
    }

    @Test
    public void shouldNotManageHooksOnNotManagedConfig() throws Exception {
        GitHubServerConfig conf = new GitHubServerConfig("");
        conf.setManageHooks(false);
        GitHubPlugin.configuration().getConfigs().add(conf);
        assertThat(GitHubPlugin.configuration().isManageHooks(), is(false));
    }
}

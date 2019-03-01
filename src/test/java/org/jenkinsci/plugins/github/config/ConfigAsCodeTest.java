package org.jenkinsci.plugins.github.config;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ConfigAsCodeTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();


    @Test
    public void shouldSupportConfigurationAsCode() throws Exception {
        ConfigurationAsCode.get().configure(
                ConfigAsCodeTest.class.getResource("configuration-as-code.yml").toString());

        GitHubPluginConfig gitHubPluginConfig = GitHubPluginConfig.all().get(GitHubPluginConfig.class);

        /** Test Global Config Properties */

        assertThat(
                "getHookUrl() is configured",
                gitHubPluginConfig.getHookUrl().toString(),
                is("http://some.com/github-webhook/secret-path")
        );

        assertThat(
                "getHookSecretConfig().getCredentialsId() is configured",
                gitHubPluginConfig.getHookSecretConfig().getCredentialsId(),
                is("hook_secret_cred_id")
        );

        /** Test GitHub Server Configs */

        List<GitHubServerConfig> gitHubServerConfigList = gitHubPluginConfig.getConfigs();
        assertThat("configs is non-empty", gitHubServerConfigList.size(), is(2));

        // First Server Config

        assertThat(
                "first server config name is configured",
                gitHubServerConfigList.get(0).getName(),
                is("Public GitHub")
        );

        assertThat(
                "first server config getApiUrl is configured",
                gitHubServerConfigList.get(0).getApiUrl(),
                is("https://api.github.com")
        );

        assertThat(
                "first server config getCredentialsId is configured",
                gitHubServerConfigList.get(0).getCredentialsId(),
                is("public_cred_id")
        );

        assertThat(
                "first server config isManageHooks is configured",
                gitHubServerConfigList.get(0).isManageHooks(),
                is(true)
        );

        assertThat(
                "second server config clientCacheSize is configured",
                gitHubServerConfigList.get(0).getClientCacheSize(),
                is(20)
        );

        // Second Server Config

        assertThat(
                "first server config name is configured",
                gitHubServerConfigList.get(0).getName(),
                is("Private GitHub")
        );

        assertThat(
                "second server config getApiUrl is configured",
                gitHubServerConfigList.get(1).getApiUrl(),
                is("https://api.some.com")
        );

        assertThat(
                "second server config getCredentialsId is configured",
                gitHubServerConfigList.get(1).getCredentialsId(),
                is("private_cred_id")
        );

        assertThat(
                "second server config isManageHooks is configured",
                gitHubServerConfigList.get(1).isManageHooks(),
                is(false)
        );

        assertThat(
                "second server config clientCacheSize is configured",
                gitHubServerConfigList.get(1).getClientCacheSize(),
                is(40)
        );
    }
}
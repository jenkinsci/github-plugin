package org.jenkinsci.plugins.github.config;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withClientCacheSize;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withCredsId;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withApiUrl;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withIsManageHooks;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withName;

public class ConfigAsCodeTest {

    @Rule
    public JenkinsConfiguredWithCodeRule r = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void shouldSupportConfigurationAsCode() throws Exception {

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

        assertThat("configs are loaded", gitHubPluginConfig.getConfigs(), hasSize(2));

        assertThat("configs are set", gitHubPluginConfig.getConfigs(), hasItems(
                both(withName(is("Public GitHub")))
                        .and(withApiUrl(is("https://api.github.com")))
                        .and(withCredsId(is("public_cred_id")))
                        .and(withClientCacheSize(is(20)))
                        .and(withIsManageHooks(is(true))),
                both(withName(is("Private GitHub")))
                        .and(withApiUrl(is("https://api.some.com")))
                        .and(withCredsId(is("private_cred_id")))
                        .and(withClientCacheSize(is(40)))
                        .and(withIsManageHooks(is(false)))
                ));
    }

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void export_configuration() throws Exception {
        /* TODO (From JCASC): Need to provide some YAML assertion library so that the resulting exported yaml
            stream can be checked for expected content. */
        ConfigurationAsCode.get().export(System.out);
    }
}
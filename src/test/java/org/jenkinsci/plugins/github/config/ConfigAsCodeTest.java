package org.jenkinsci.plugins.github.config;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.Configurator;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withApiUrl;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withApiUrlS;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withClientCacheSize;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withClientCacheSizeS;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withCredsId;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withCredsIdS;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withIsManageHooks;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withIsManageHooksS;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withName;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withNameS;

@WithJenkinsConfiguredWithCode
class ConfigAsCodeTest {

    @SuppressWarnings("deprecation")
    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    void shouldSupportConfigurationAsCode(JenkinsConfiguredWithCodeRule r) throws Exception {

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
    void exportConfiguration(JenkinsConfiguredWithCodeRule r) throws Exception {
        GitHubPluginConfig globalConfiguration = GitHubPluginConfig.all().get(GitHubPluginConfig.class);

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final Configurator c = context.lookupOrFail(GitHubPluginConfig.class);

        @SuppressWarnings("unchecked")
        CNode node = c.describe(globalConfiguration, context);
        assertThat(node, notNullValue());
        final Mapping mapping = node.asMapping();

        assertThat(mapping.getScalarValue("hookUrl"), is("http://some.com/github-webhook/secret-path"));

        CNode configsNode = mapping.get("configs");
        assertThat(configsNode, notNullValue());

        List<Mapping> configsMapping = (List) configsNode.asSequence();
        assertThat(configsMapping, hasSize(2));

        assertThat("configs are set", configsMapping,
                hasItems(
                        both(withCredsIdS(is("public_cred_id")))
                                .and(withNameS(is("Public GitHub"))),
                        both(withNameS(is("Private GitHub")))
                                .and(withApiUrlS(is("https://api.some.com")))
                                .and(withCredsIdS(is("private_cred_id")))
                                .and(withClientCacheSizeS(is(40)))
                                .and(withIsManageHooksS(is(false)))
                )
        );
    }
}
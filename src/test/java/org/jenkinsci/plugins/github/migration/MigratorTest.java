package org.jenkinsci.plugins.github.migration;

import com.cloudbees.jenkins.Credential;
import com.cloudbees.jenkins.GitHubPushTrigger;
import com.cloudbees.jenkins.GitHubWebHook;
import hudson.model.FreeStyleProject;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.IOException;

import static java.lang.String.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.GITHUB_URL;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withApiUrl;
import static org.jenkinsci.plugins.github.test.GitHubServerConfigMatcher.withCredsWithToken;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class MigratorTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    public static final String HOOK_FROM_LOCAL_DATA = "http://some.proxy.example.com/webhook";
    public static final String CUSTOM_GH_URL = "http://custom.github.example.com/api/v3";
    public static final String TOKEN = "some-oauth-token";
    public static final String TOKEN2 = "some-oauth-token2";
    public static final String TOKEN3 = "some-oauth-token3";

    /**
     * Just ignore malformed hook in old config
     */
    @Test
    @LocalData
    public void shouldNotThrowExcMalformedHookUrlInOldConfig() throws IOException {
        FreeStyleProject job = jenkins.createFreeStyleProject();
        GitHubPushTrigger trigger = new GitHubPushTrigger();
        trigger.start(job, true);
        trigger.registerHooks();

        assertThat("self hook url", trigger.getDescriptor().getDeprecatedHookUrl(), nullValue());
        assertThat("imported hook url", valueOf(trigger.getDescriptor().getHookUrl()),
                containsString(Jenkins.getInstance().getRootUrl() + GitHubWebHook.URLNAME));
        assertThat("in plugin - override", GitHubPlugin.configuration().isOverrideHookUrl(), is(false));
    }

    @Test
    @LocalData
    public void shouldMigrateHookUrl() {
        assertThat("in plugin - override", GitHubPlugin.configuration().isOverrideHookUrl(), is(true));
        assertThat("in plugin", valueOf(GitHubPlugin.configuration().getHookUrl()), is(HOOK_FROM_LOCAL_DATA));

        assertThat("should nullify hook url after migration",
                GitHubPushTrigger.DescriptorImpl.get().getDeprecatedHookUrl(), nullValue());
    }

    @Test
    @LocalData
    public void shouldMigrateCredentials() throws Exception {
        assertThat("should migrate 3 configs", GitHubPlugin.configuration().getConfigs(), hasSize(3));
        assertThat("migrate custom url", GitHubPlugin.configuration().getConfigs(), hasItems(
                both(withApiUrl(is(CUSTOM_GH_URL))).and(withCredsWithToken(TOKEN2)),
                both(withApiUrl(is(GITHUB_URL))).and(withCredsWithToken(TOKEN)),
                both(withApiUrl(is(GITHUB_URL))).and(withCredsWithToken(TOKEN3))
        ));
    }

    @Ignore("TODO the XStream alias doesn't seem to be working")
    @Test
    @LocalData
    public void shouldLoadDataAfterStart() throws Exception {
        assertThat("should load 2 configs", GitHubPlugin.configuration().getConfigs(), hasSize(2));
        assertThat("migrate custom url", GitHubPlugin.configuration().getConfigs(), hasItems(
                withApiUrl(is(CUSTOM_GH_URL)),
                withApiUrl(is(GITHUB_URL))
        ));
        assertThat("should load hook url",
                GitHubPlugin.configuration().getHookUrl().toString(), equalTo(HOOK_FROM_LOCAL_DATA));
    }

    @Test
    public void shouldConvertCredsToServerConfig() throws Exception {
        GitHubServerConfig conf = new Migrator().toGHServerConfig()
                .apply(new Credential("name", CUSTOM_GH_URL, "token"));
        assertThat(conf, both(withCredsWithToken("token")).and(withApiUrl(is(CUSTOM_GH_URL))));
    }
}

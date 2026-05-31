package com.cloudbees.jenkins;

import org.jenkinsci.plugins.github.GitHubPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

/**
 * Test Class for {@link GitHubPushTrigger}.
 *
 * @author Seiji Sogabe
 */
@WithJenkins
class GlobalConfigSubmitTest {

    private static final String WEBHOOK_URL = "http://jenkinsci.example.com/jenkins/github-webhook/";

    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkins = rule;
    }

    @Test
    void shouldSetHookUrl() throws Exception {
        GitHubPlugin.configuration().setOverrideHookUrl(true);
        GitHubPlugin.configuration().setHookUrl(WEBHOOK_URL);
        GitHubPlugin.configuration().save();
        GitHubPlugin.configuration().load();

        assertThat(GitHubPlugin.configuration().getHookUrl(), equalTo(new URL(WEBHOOK_URL)));
    }

    @Test
    void shouldResetHookUrlIfNotChecked() throws Exception {
        GitHubPlugin.configuration().setHookUrl(WEBHOOK_URL);
        GitHubPlugin.configuration().setHookUrl(null);
        GitHubPlugin.configuration().save();
        GitHubPlugin.configuration().load();

        assertThat(GitHubPlugin.configuration().getHookUrl().toString(), startsWith(jenkins.jenkins.getRootUrl()));
    }
}

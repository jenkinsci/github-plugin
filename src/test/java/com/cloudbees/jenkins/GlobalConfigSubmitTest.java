package com.cloudbees.jenkins;

import jenkins.model.Jenkins;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlTextInput;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsSessionRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Test Class for {@link GitHubPushTrigger}.
 *
 * @author Seiji Sogabe, Frank Genois
 */
public class GlobalConfigSubmitTest {
    public static final String HOOK_URL_INPUT = "_.hookUrl";

    @Rule
    public JenkinsSessionRule sessions = new JenkinsSessionRule();

    @Test
    public void shouldBeAbleToParseAnEmptyConfig() throws Throwable {
        sessions.then(r -> {
            GitHubPluginConfig githubPluginConfig = GitHubPlugin.configuration();
            assertThat("empty config by default", githubPluginConfig.getConfigs().isEmpty());
            assertFalse("no override hook url by default", githubPluginConfig.isOverrideHookUrl());
            assertEquals(
                "uses default url by default",
                getDefaultHookUrl(),
                githubPluginConfig.getHookUrl().toString()
            );
        });
    }

    @Test
    public void shouldBeAbleToSetCustomHook() throws Throwable {
        sessions.then(r -> {
            HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
            HtmlTextInput textbox = config.getInputByName(HOOK_URL_INPUT);
            textbox.setText("http://jenkinsci.example.com/jenkins/github-webhook/");
            r.submit(config);

            assertEquals(
                "global config page let us edit the webhook url",
                "http://jenkinsci.example.com/jenkins/github-webhook/",
                GitHubPlugin.configuration().getHookUrl().toString()
            );
        });

        sessions.then(r -> {
            assertEquals(
                    "webhook url still present after restart of Jenkins",
                    "http://jenkinsci.example.com/jenkins/github-webhook/",
                    GitHubPlugin.configuration().getHookUrl().toString()
            );
        });
    }

    @Test
    public void shouldSetDefaultHookUrlWhenLeftEmpty() throws Throwable {
        sessions.then(r -> {
            HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
            HtmlTextInput textbox = config.getInputByName(HOOK_URL_INPUT);
            textbox.setText("");
            r.submit(config);

            assertEquals(
                    "global config page let us edit the webhook url",
                    getDefaultHookUrl(),
                    GitHubPlugin.configuration().getHookUrl().toString()
            );
        });
    }

    @Test
    public void shouldSetDefaultHookUrlWhenGivenMalformedUrl() throws Throwable {
        sessions.then(r -> {
            HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
            HtmlTextInput textbox = config.getInputByName(HOOK_URL_INPUT);
            textbox.setText("I'm not a URL!");
            r.submit(config);

            assertEquals(
                    "global config page let us edit the webhook url",
                    getDefaultHookUrl(),
                    GitHubPlugin.configuration().getHookUrl().toString()
            );
        });
    }

    public String getDefaultHookUrl() {
        return Jenkins.getInstance().getRootUrl() + "github-webhook/";
    }
}

package com.cloudbees.jenkins;

import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test Class for {@link GitHubPushTrigger}.
 *
 * @author Seiji Sogabe
 */
@WithJenkins
@Disabled("Have troubles with memory consumption")
public class GlobalConfigSubmitTest {

    public static final String OVERRIDE_HOOK_URL_CHECKBOX = "_.isOverrideHookUrl";
    public static final String HOOK_URL_INPUT = "_.hookUrl";

    private static final String WEBHOOK_URL = "http://jenkinsci.example.com/jenkins/github-webhook/";

    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkins = rule;
    }

    @Test
    void shouldSetHookUrl() throws Exception {
        HtmlForm form = globalConfig();

        form.getInputByName(OVERRIDE_HOOK_URL_CHECKBOX).setChecked(true);
        form.getInputByName(HOOK_URL_INPUT).setValue(WEBHOOK_URL);
        jenkins.submit(form);

        assertThat(GitHubPlugin.configuration().getHookUrl(), equalTo(new URL(WEBHOOK_URL)));
    }

    @Test
    void shouldNotSetHookUrl() throws Exception {
        GitHubPlugin.configuration().setHookUrl(WEBHOOK_URL);

        HtmlForm form = globalConfig();

        form.getInputByName(OVERRIDE_HOOK_URL_CHECKBOX).setChecked(false);
        form.getInputByName(HOOK_URL_INPUT).setValue("http://foo");
        jenkins.submit(form);

        assertThat(GitHubPlugin.configuration().getHookUrl(), equalTo(new URL(WEBHOOK_URL)));
    }

    @Test
    void shouldNotOverrideAPreviousHookUrlIfNotChecked() throws Exception {
        GitHubPlugin.configuration().setHookUrl(WEBHOOK_URL);

        HtmlForm form = globalConfig();

        form.getInputByName(OVERRIDE_HOOK_URL_CHECKBOX).setChecked(false);
        form.getInputByName(HOOK_URL_INPUT).setValue("");
        jenkins.submit(form);

        assertThat(GitHubPlugin.configuration().getHookUrl(), equalTo(new URL(WEBHOOK_URL)));
    }

    public HtmlForm globalConfig() throws IOException, SAXException {
        JenkinsRule.WebClient client = configureWebClient();
        HtmlPage p = client.goTo("configure");
        return p.getFormByName("config");
    }

    private JenkinsRule.WebClient configureWebClient() {
        JenkinsRule.WebClient client = jenkins.createWebClient();
        client.setJavaScriptEnabled(true);
        return client;
    }
}

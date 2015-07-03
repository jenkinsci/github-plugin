package com.cloudbees.jenkins;

import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.util.Secret;
import org.jenkinsci.plugins.github.internal.GHPluginConfigException;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import org.kohsuke.stapler.Stapler;

import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test Class for {@link GitHubPushTrigger}.
 *
 * @author Seiji Sogabe
 */
public class GitHubPushTriggerConfigSubmitTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private static final String WEBHOOK_URL = "http://jenkinsci.example.com/jenkins/github-webhook/";

    @Test
    public void testConfigSubmit_AutoManageHook() throws Exception {

        JenkinsRule.WebClient client = configureWebClient();
        HtmlPage p = client.goTo("configure");
        HtmlForm f = p.getFormByName("config");
        f.getInputByValue("auto").setChecked(true);
        f.getInputByName("_.hasHookUrl").setChecked(true);
        f.getInputByName("_.hookUrl").setValueAttribute(WEBHOOK_URL);
        f.getInputByName("_.username").setValueAttribute("jenkins");
        jenkins.submit(f);

        GitHubPushTrigger.DescriptorImpl d = getDescriptor();
        assertTrue(d.isManageHook());
        assertEquals(new URL(WEBHOOK_URL), d.getHookUrl());

        List<Credential> credentials = d.getCredentials();
        assertNotNull(credentials);
        assertEquals(1, credentials.size());
        Credential credential = credentials.get(0);
        assertEquals("jenkins", credential.username);
    }

    @Test
    public void testConfigSubmit_ManuallyManageHook() throws Exception {
        JenkinsRule.WebClient client = configureWebClient();
        HtmlPage p = client.goTo("configure");
        HtmlForm f = p.getFormByName("config");
        f.getInputByValue("none").setChecked(true);
        jenkins.submit(f);

        GitHubPushTrigger.DescriptorImpl d = getDescriptor();
        assertFalse(d.isManageHook());
    }

    @Test
    @LocalData
    public void shouldDontThrowExcMailformedHookUrl() {
        new GitHubPushTrigger().registerHooks();
    }

    @Test(expected = GHPluginConfigException.class)
    @LocalData
    public void shouldThrowExcMailformedHookUrlGetter() {
        new GitHubPushTrigger().getDescriptor().getHookUrl();
    }

    private GitHubPushTrigger.DescriptorImpl getDescriptor() {
        return (GitHubPushTrigger.DescriptorImpl) GitHubPushTrigger.DescriptorImpl.get();
    }

    private JenkinsRule.WebClient configureWebClient() {
        JenkinsRule.WebClient client = jenkins.createWebClient();
        client.setThrowExceptionOnFailingStatusCode(false);
        client.setCssEnabled(false);
        client.setJavaScriptEnabled(true);
        return client;
    }

    // workaround
    static {
        Stapler.CONVERT_UTILS.register(new org.apache.commons.beanutils.Converter() {

            public Secret convert(Class type, Object value) {
                return Secret.fromString(value.toString());
            }
        }, Secret.class);
    }
}

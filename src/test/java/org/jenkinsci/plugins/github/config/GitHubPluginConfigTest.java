package org.jenkinsci.plugins.github.config;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebRequest;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubPluginConfigTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

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

    @Test
    @Issue("SECURITY-799")
    public void shouldNotAllowSSRFUsingHookUrl() throws Exception {
        final String targetUrl = "www.google.com";
        final URL urlForSSRF = new URL(j.getURL() + "descriptorByName/github-plugin-configuration/checkHookUrl?value=" + targetUrl);
        
        j.jenkins.setCrumbIssuer(null);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        
        GlobalMatrixAuthorizationStrategy strategy = new GlobalMatrixAuthorizationStrategy();
        strategy.add(Jenkins.ADMINISTER, "admin");
        strategy.add(Jenkins.READ, "user");
        j.jenkins.setAuthorizationStrategy(strategy);
        
        { // as read-only user
            JenkinsRule.WebClient wc = j.createWebClient();
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.login("user");
            
            Page page = wc.getPage(new WebRequest(urlForSSRF, HttpMethod.POST));
            assertThat(page.getWebResponse().getStatusCode(), equalTo(403));
        }
        { // as admin
            JenkinsRule.WebClient wc = j.createWebClient();
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.login("admin");
            
            Page page = wc.getPage(new WebRequest(urlForSSRF, HttpMethod.POST));
            assertThat(page.getWebResponse().getStatusCode(), equalTo(200));
        }
        {// even admin must use POST
            JenkinsRule.WebClient wc = j.createWebClient();
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.login("admin");
            
            Page page = wc.getPage(new WebRequest(urlForSSRF, HttpMethod.GET));
            assertThat(page.getWebResponse().getStatusCode(), not(equalTo(200)));
        }
    }

    @Test
    @Issue("JENKINS-62097")
    public void configRoundtrip() throws Exception {
        assertHookSecrets("");
        j.configRoundtrip();
        assertHookSecrets("");
        SystemCredentialsProvider.getInstance().setDomainCredentialsMap(Collections.singletonMap(Domain.global(), Arrays.asList(
                new StringCredentialsImpl(CredentialsScope.SYSTEM, "one", null, Secret.fromString("#1")),
                new StringCredentialsImpl(CredentialsScope.SYSTEM, "two", null, Secret.fromString("#2")))));
        GitHubPlugin.configuration().setHookSecretConfigs(Arrays.asList(new HookSecretConfig("one"), new HookSecretConfig("two")));
        assertHookSecrets("#1; #2");
        j.configRoundtrip();
        assertHookSecrets("#1; #2");
    }
    private void assertHookSecrets(String expected) {
        assertEquals(expected, GitHubPlugin.configuration().getHookSecretConfigs().stream().map(HookSecretConfig::getHookSecret).filter(Objects::nonNull).map(Secret::getPlainText).collect(Collectors.joining("; ")));
    }

}

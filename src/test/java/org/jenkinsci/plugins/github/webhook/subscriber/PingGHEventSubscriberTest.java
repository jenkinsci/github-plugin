package org.jenkinsci.plugins.github.webhook.subscriber;

import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.kohsuke.github.GHEvent;

import static com.cloudbees.jenkins.GitHubWebHookFullTest.classpath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.jvnet.hudson.test.Issue;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class PingGHEventSubscriberTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void shouldBeNotApplicableForProjects() throws Exception {
        FreeStyleProject prj = jenkins.createFreeStyleProject();
        assertThat(new PingGHEventSubscriber().isApplicable(prj), is(false));
    }

    @Test
    public void shouldParsePingPayload() throws Exception {
        injectedPingSubscr().onEvent(GHEvent.PING, classpath("payloads/ping.json"), null);
    }

    @Issue("JENKINS-30626")
    @Test
    @WithoutJenkins
    public void shouldParseOrgPingPayload() throws Exception {
        new PingGHEventSubscriber().onEvent(GHEvent.PING, classpath("payloads/orgping.json"), null);
    }
    
    private PingGHEventSubscriber injectedPingSubscr() {
        PingGHEventSubscriber pingSubsc = new PingGHEventSubscriber();
        jenkins.getInstance().getInjector().injectMembers(pingSubsc);
        return pingSubsc;
    }

}

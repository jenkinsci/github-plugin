package org.jenkinsci.plugins.github.webhook.subscriber;

import hudson.model.FreeStyleProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.github.GHEvent;

import static com.cloudbees.jenkins.GitHubWebHookFullTest.classpath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author lanwen (Merkushev Kirill)
 */
@WithJenkins
class PingGHEventSubscriberTest {

    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkins = rule;
    }

    @Test
    void shouldBeNotApplicableForProjects() throws Exception {
        FreeStyleProject prj = jenkins.createFreeStyleProject();
        assertThat(new PingGHEventSubscriber().isApplicable(prj), is(false));
    }

    @Test
    void shouldParsePingPayload() throws Exception {
        injectedPingSubscr().onEvent(GHEvent.PING, classpath("payloads/ping.json"));
    }

    @Issue("JENKINS-30626")
    @Test
    @WithoutJenkins
    void shouldParseOrgPingPayload() throws Exception {
        new PingGHEventSubscriber().onEvent(GHEvent.PING, classpath("payloads/orgping.json"));
    }

    private PingGHEventSubscriber injectedPingSubscr() {
        PingGHEventSubscriber pingSubsc = new PingGHEventSubscriber();
        jenkins.getInstance().getInjector().injectMembers(pingSubsc);
        return pingSubsc;
    }

}

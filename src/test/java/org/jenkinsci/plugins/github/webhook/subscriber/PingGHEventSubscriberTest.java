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
    @WithoutJenkins
    public void shouldParsePingPayload() throws Exception {
        new PingGHEventSubscriber().onEvent(GHEvent.PING, classpath("payloads/ping.json"));
    }
}

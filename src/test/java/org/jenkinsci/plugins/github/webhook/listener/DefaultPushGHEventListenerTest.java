package org.jenkinsci.plugins.github.webhook.listener;

import com.cloudbees.jenkins.GitHubPushTrigger;
import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class DefaultPushGHEventListenerTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void shouldBeNotApplicableForProjectWithoutTrigger() throws Exception {
        FreeStyleProject prj = jenkins.createFreeStyleProject();
        assertThat(new DefaultPushGHEventListener().isApplicable(prj), is(false));
    }

    @Test
    public void shouldBeApplicableForProjectWithTrigger() throws Exception {
        FreeStyleProject prj = jenkins.createFreeStyleProject();
        prj.addTrigger(new GitHubPushTrigger());
        assertThat(new DefaultPushGHEventListener().isApplicable(prj), is(true));
    }
}

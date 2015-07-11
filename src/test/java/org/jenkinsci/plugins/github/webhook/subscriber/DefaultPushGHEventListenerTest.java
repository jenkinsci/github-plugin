package org.jenkinsci.plugins.github.webhook.subscriber;

import com.cloudbees.jenkins.GitHubPushTrigger;
import com.cloudbees.jenkins.GitHubWebHookFullTest;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GHEvent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class DefaultPushGHEventListenerTest {

    public static final GitSCM GIT_SCM_FROM_RESOURCE = new GitSCM("ssh://git@github.com/lanwen/test.git");
    public static final String TRIGGERED_BY_USER_FROM_RESOURCE = "lanwen";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void shouldBeNotApplicableForProjectWithoutTrigger() throws Exception {
        FreeStyleProject prj = jenkins.createFreeStyleProject();
        assertThat(new DefaultPushGHEventSubscriber().isApplicable(prj), is(false));
    }

    @Test
    public void shouldBeApplicableForProjectWithTrigger() throws Exception {
        FreeStyleProject prj = jenkins.createFreeStyleProject();
        prj.addTrigger(new GitHubPushTrigger());
        assertThat(new DefaultPushGHEventSubscriber().isApplicable(prj), is(true));
    }

    @Test
    public void shouldParsePushPayload() throws Exception {
        GitHubPushTrigger trigger = mock(GitHubPushTrigger.class);
        
        FreeStyleProject prj = jenkins.createFreeStyleProject();
        prj.addTrigger(trigger);
        prj.setScm(GIT_SCM_FROM_RESOURCE);
        
        new DefaultPushGHEventSubscriber()
                .onEvent(GHEvent.PUSH, GitHubWebHookFullTest.classpath("payloads/push.json"));
        
        verify(trigger).onPost(TRIGGERED_BY_USER_FROM_RESOURCE);
    }
}

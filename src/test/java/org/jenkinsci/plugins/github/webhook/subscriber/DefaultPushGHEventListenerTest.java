package org.jenkinsci.plugins.github.webhook.subscriber;

import com.cloudbees.jenkins.GitHubPushTrigger;
import com.cloudbees.jenkins.GitHubTriggerEvent;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GHEvent;

import static com.cloudbees.jenkins.GitHubWebHookFullTest.classpath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
                .onEvent("shouldParsePushPayload", GHEvent.PUSH, classpath("payloads/push.json"));

        verify(trigger).onPost(GitHubTriggerEvent.create()
                .withOrigin("shouldParsePushPayload")
                .withTriggeredByUser(TRIGGERED_BY_USER_FROM_RESOURCE)
                .build()
        );
    }

    @Test
    @Issue("JENKINS-27136")
    public void shouldReceivePushHookOnWorkflow() throws Exception {
        WorkflowJob job = jenkins.getInstance().createProject(WorkflowJob.class, "test-workflow-job");

        GitHubPushTrigger trigger = mock(GitHubPushTrigger.class);
        job.addTrigger(trigger);
        job.setDefinition(new CpsFlowDefinition(classpath(getClass(), "workflow-definition.groovy")));
        // Trigger the build once to register SCMs
        jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));

        new DefaultPushGHEventSubscriber()
                .onEvent("shouldReceivePushHookOnWorkflow", GHEvent.PUSH, classpath("payloads/push.json"));

        verify(trigger).onPost(GitHubTriggerEvent.create()
                .withOrigin("shouldReceivePushHookOnWorkflow")
                .withTriggeredByUser(TRIGGERED_BY_USER_FROM_RESOURCE)
                .build()
        );
    }

    @Test
    @Issue("JENKINS-27136")
    public void shouldNotReceivePushHookOnWorkflowWithNoBuilds() throws Exception {
        WorkflowJob job = jenkins.getInstance().createProject(WorkflowJob.class, "test-workflow-job");

        GitHubPushTrigger trigger = mock(GitHubPushTrigger.class);
        job.addTrigger(trigger);
        job.setDefinition(new CpsFlowDefinition(classpath(getClass(), "workflow-definition.groovy")));

        new DefaultPushGHEventSubscriber()
                .onEvent("shouldNotReceivePushHookOnWorkflowWithNoBuilds", GHEvent.PUSH, classpath("payloads/push.json"));

        verify(trigger, never()).onPost(GitHubTriggerEvent.create()
                .withOrigin("shouldNotReceivePushHookOnWorkflowWithNoBuilds")
                .withTriggeredByUser(TRIGGERED_BY_USER_FROM_RESOURCE)
                .build()
        );
    }
}

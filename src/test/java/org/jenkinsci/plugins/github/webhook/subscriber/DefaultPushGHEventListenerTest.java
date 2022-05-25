package org.jenkinsci.plugins.github.webhook.subscriber;

import com.cloudbees.jenkins.GitHubPushTrigger;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;
import com.cloudbees.jenkins.GitHubTriggerEvent;
import com.cloudbees.jenkins.GitHubWebHook;

import hudson.ExtensionList;
import hudson.model.EnvironmentContributor;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.plugins.git.GitSCM;
import java.util.Collections;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.kohsuke.github.GHEvent;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static com.cloudbees.jenkins.GitHubWebHookFullTest.classpath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class DefaultPushGHEventListenerTest {

    public static final GitSCM GIT_SCM_FROM_RESOURCE = new GitSCM("ssh://git@github.com/lanwen/test.git");
    public static final String TRIGGERED_BY_USER_FROM_RESOURCE = "lanwen";
    public static final String TRIGGERED_BY_REF_FROM_RESOURCE = "refs/heads/master";    

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    @WithoutJenkins
    public void shouldBeNotApplicableForProjectWithoutTrigger() {
        FreeStyleProject prj = mock(FreeStyleProject.class);
        assertThat(new DefaultPushGHEventSubscriber().isApplicable(prj), is(false));
    }

    @Test
    @WithoutJenkins
    public void shouldBeApplicableForProjectWithTrigger() {
        FreeStyleProject prj = mock(FreeStyleProject.class);
        when(prj.getTriggers()).thenReturn(
                Collections.singletonMap(new GitHubPushTrigger.DescriptorImpl(), new GitHubPushTrigger()));
        assertThat(new DefaultPushGHEventSubscriber().isApplicable(prj), is(true));
    }

    @Test
    @WithoutJenkins
    public void shouldParsePushPayload() {
        GitHubPushTrigger trigger = mock(GitHubPushTrigger.class);

        FreeStyleProject prj = mock(FreeStyleProject.class);
        when(prj.getTriggers()).thenReturn(
                Collections.singletonMap(new GitHubPushTrigger.DescriptorImpl(), trigger));
        when(prj.getSCMs()).thenAnswer(unused -> Collections.singletonList(GIT_SCM_FROM_RESOURCE));

        GHSubscriberEvent subscriberEvent =
                new GHSubscriberEvent("shouldParsePushPayload", GHEvent.PUSH, classpath("payloads/push.json"));

        Jenkins jenkins = mock(Jenkins.class);
        when(jenkins.getAllItems(Item.class)).thenReturn(Collections.singletonList(prj));

        {
            ExtensionList<GitHubRepositoryNameContributor> extensionList = mock(ExtensionList.class);
            List<GitHubRepositoryNameContributor> gitHubRepositoryNameContributorList =
                Collections.singletonList(new GitHubRepositoryNameContributor.FromSCM());
            when(extensionList.iterator()).thenReturn(gitHubRepositoryNameContributorList.iterator());
            when(jenkins.getExtensionList(GitHubRepositoryNameContributor.class)).thenReturn(extensionList);
        }

        {
            ExtensionList<EnvironmentContributor> extensionList = mock(ExtensionList.class);
            List<EnvironmentContributor> environmentContributorList = Collections.emptyList();
            when(extensionList.iterator()).thenReturn(environmentContributorList.iterator());
            when(jenkins.getExtensionList(EnvironmentContributor.class)).thenReturn(extensionList);
        }

        {
            ExtensionList<GitHubWebHook.Listener> extensionList = mock(ExtensionList.class);
            List<GitHubWebHook.Listener> listenerList = Collections.emptyList();
            when(extensionList.iterator()).thenReturn(listenerList.iterator());
            when(jenkins.getExtensionList(GitHubWebHook.Listener.class)).thenReturn(extensionList);
        }

        try (MockedStatic<Jenkins> mockedJenkins = mockStatic(Jenkins.class)) {
            mockedJenkins.when(Jenkins::getInstance).thenReturn(jenkins);
            mockedJenkins.when(Jenkins::getInstanceOrNull).thenReturn(jenkins);
            new DefaultPushGHEventSubscriber().onEvent(subscriberEvent);
        }

        verify(trigger).onPost(eq(GitHubTriggerEvent.create()
                .withTimestamp(subscriberEvent.getTimestamp())
                .withOrigin("shouldParsePushPayload")
                .withTriggeredByUser(TRIGGERED_BY_USER_FROM_RESOURCE)
                .withTriggeredByRef(TRIGGERED_BY_REF_FROM_RESOURCE)
                .build()
        ));
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

        GHSubscriberEvent subscriberEvent =
                new GHSubscriberEvent("shouldReceivePushHookOnWorkflow", GHEvent.PUSH, classpath("payloads/push.json"));
        new DefaultPushGHEventSubscriber().onEvent(subscriberEvent);

        verify(trigger).onPost(eq(GitHubTriggerEvent.create()
                .withTimestamp(subscriberEvent.getTimestamp())
                .withOrigin("shouldReceivePushHookOnWorkflow")
                .withTriggeredByUser(TRIGGERED_BY_USER_FROM_RESOURCE)
                .withTriggeredByRef(TRIGGERED_BY_REF_FROM_RESOURCE)
                .build()
        ));
    }

    @Test
    @Issue("JENKINS-27136")
    public void shouldNotReceivePushHookOnWorkflowWithNoBuilds() throws Exception {
        WorkflowJob job = jenkins.getInstance().createProject(WorkflowJob.class, "test-workflow-job");

        GitHubPushTrigger trigger = mock(GitHubPushTrigger.class);
        job.addTrigger(trigger);
        job.setDefinition(new CpsFlowDefinition(classpath(getClass(), "workflow-definition.groovy")));

        GHSubscriberEvent subscriberEvent =
                new GHSubscriberEvent("shouldNotReceivePushHookOnWorkflowWithNoBuilds", GHEvent.PUSH,
                        classpath("payloads/push.json"));
        new DefaultPushGHEventSubscriber().onEvent(subscriberEvent);

        verify(trigger, never()).onPost(Mockito.any(GitHubTriggerEvent.class));
    }
}

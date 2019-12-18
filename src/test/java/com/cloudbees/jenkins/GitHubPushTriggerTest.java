package com.cloudbees.jenkins;

import static com.cloudbees.jenkins.GitHubWebHookFullTest.classpath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.webhook.subscriber.DefaultPushGHEventListenerTest.TRIGGERED_BY_USER_FROM_RESOURCE;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.github.admin.GitHubHookRegisterProblemMonitor;
import org.jenkinsci.plugins.github.webhook.subscriber.DefaultPushGHEventListenerTest;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;

import com.cloudbees.jenkins.GitHubPushTrigger.DescriptorImpl;

import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.extensions.impl.UserExclusion;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import hudson.util.FormValidation;
import hudson.util.ReflectionUtils;
import hudson.util.SequentialExecutionQueue;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubPushTriggerTest {
    private static final GitHubRepositoryName REPO = new GitHubRepositoryName("host", "user", "repo");
    private static final GitSCM REPO_GIT_SCM = new GitSCM("git://host/user/repo.git");

    @Inject
    private GitHubHookRegisterProblemMonitor monitor;

    @Inject
    private GitHubPushTrigger.DescriptorImpl descriptor;

    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        jRule.getInstance().getInjector().injectMembers(this);
    }

    /**
     * This test requires internet access to get real git revision
     */
    @Test
    @Issue("JENKINS-27136")
    public void shouldStartWorkflowByTrigger() throws Exception {
        WorkflowJob job = jRule.getInstance().createProject(WorkflowJob.class, "test-workflow-job");
        GitHubPushTrigger trigger = new GitHubPushTrigger();
        trigger.start(job, false);
        job.addTrigger(trigger);
        job.setDefinition(
                new CpsFlowDefinition(classpath(DefaultPushGHEventListenerTest.class, "workflow-definition.groovy"))
        );

        // Trigger the build once to register SCMs
        WorkflowRun lastRun = jRule.assertBuildStatusSuccess(job.scheduleBuild2(0));
        // Testing hack! This will make the polling believe that there was remote changes to build
        BuildData buildData = lastRun.getActions(BuildData.class).get(0);
        buildData.buildsByBranchName = new HashMap<String, Build>();
        buildData.getLastBuiltRevision().setSha1(ObjectId.zeroId());

        trigger.onPost(TRIGGERED_BY_USER_FROM_RESOURCE);

        TimeUnit.SECONDS.sleep(job.getQuietPeriod());
        jRule.waitUntilNoActivity();

        assertThat("should be 2 build after hook", job.getLastBuild().getNumber(), is(2));
    }

    @Test
    @Issue("JENKINS-24690")
    public void shouldReturnWaringOnHookProblem() throws Exception {
        monitor.registerProblem(REPO, new IOException());
        FreeStyleProject job = jRule.createFreeStyleProject();
        job.setScm(REPO_GIT_SCM);

        FormValidation validation = descriptor.doCheckHookRegistered(job);
        assertThat("warning", validation.kind, is(FormValidation.Kind.WARNING));
    }

    @Test
    public void shouldReturnOkOnNoAnyProblem() throws Exception {
        FreeStyleProject job = jRule.createFreeStyleProject();
        job.setScm(REPO_GIT_SCM);

        FormValidation validation = descriptor.doCheckHookRegistered(job);
        assertThat("all ok", validation.kind, is(FormValidation.Kind.OK));
    }
    
    private SequentialExecutionQueue addSpyToQueueField() {
        Field queueField = ReflectionUtils.findField(DescriptorImpl.class, "queue");
        ReflectionUtils.makeAccessible(queueField);
        SequentialExecutionQueue queue = (SequentialExecutionQueue)ReflectionUtils.getField(queueField, descriptor);
        SequentialExecutionQueue spiedQueue = Mockito.spy(queue);
        ReflectionUtils.setField(queueField, descriptor, spiedQueue);
        return spiedQueue;
    }

    @Test
    public void shouldSkipBuildIfExclusionEnabledWithMatchingUser() throws IOException {
        SequentialExecutionQueue spiedQueue = addSpyToQueueField();

        String matchingUserName = "userName";
        FreeStyleProject project = jRule.createFreeStyleProject();
        GitHubPushTrigger trigger = new GitHubPushTrigger();
        trigger.setUseGitExcludedUsers(true);
        trigger.start(project, false);
        project.addTrigger(trigger);
        GitSCM scm = new GitSCM("https://localhost/dummy.git");
        UserExclusion userExclusion = new UserExclusion("something" + System.lineSeparator() +
                                                        matchingUserName + System.lineSeparator() +
                                                        "somethingElse" + System.lineSeparator()); 
        scm.getExtensions().add(userExclusion);
        project.setScm(scm);
        
        GitHubTriggerEvent event = GitHubTriggerEvent.create()
                                                     .withTimestamp(System.currentTimeMillis())
                                                     .withOrigin("origin")
                                                     .withTriggeredByUser(matchingUserName)
                                                     .build();
        trigger.onPost(event);
        
        verify(spiedQueue, times(0)).execute(Mockito.any(Runnable.class));
    }

    @Test
    public void shouldSkipBuildIfExclusionEnabledWithMatchingUserCaseInsensitive() throws IOException {
        SequentialExecutionQueue spiedQueue = addSpyToQueueField();

        String matchingUserName = "userName".toLowerCase();
        FreeStyleProject project = jRule.createFreeStyleProject();
        GitHubPushTrigger trigger = new GitHubPushTrigger();
        trigger.setUseGitExcludedUsers(true);
        trigger.start(project, false);
        project.addTrigger(trigger);
        GitSCM scm = new GitSCM("https://localhost/dummy.git");
        UserExclusion userExclusion = new UserExclusion("something" + System.lineSeparator() +
                                                        matchingUserName.toUpperCase() + System.lineSeparator() +
                                                        "somethingElse" + System.lineSeparator()); 
        scm.getExtensions().add(userExclusion);
        project.setScm(scm);
        
        GitHubTriggerEvent event = GitHubTriggerEvent.create()
                                                     .withTimestamp(System.currentTimeMillis())
                                                     .withOrigin("origin")
                                                     .withTriggeredByUser(matchingUserName)
                                                     .build();
        trigger.onPost(event);
        
        verify(spiedQueue, times(0)).execute(Mockito.any(Runnable.class));
    }

    @Test
    public void shouldTriggerBuildIfExclusionEnabledWithNonMatchingUser() throws IOException {
        SequentialExecutionQueue spiedQueue = addSpyToQueueField();
        
        FreeStyleProject project = jRule.createFreeStyleProject();
        GitHubPushTrigger trigger = new GitHubPushTrigger();
        trigger.setUseGitExcludedUsers(true);
        trigger.start(project, false);
        project.addTrigger(trigger);
        GitSCM scm = new GitSCM("https://localhost/dummy.git");
        UserExclusion userExclusion = new UserExclusion("something" + System.lineSeparator() +
                                                        "nonMatchingUserName" + System.lineSeparator() +
                                                        "somethingElse" + System.lineSeparator()); 
        scm.getExtensions().add(userExclusion);
        project.setScm(scm);
        
        GitHubTriggerEvent event = GitHubTriggerEvent.create()
                                                     .withTimestamp(System.currentTimeMillis())
                                                     .withOrigin("origin")
                                                     .withTriggeredByUser("userName")
                                                     .build();
        trigger.onPost(event);
        
        verify(spiedQueue).execute(Mockito.any(Runnable.class));
    }

    @Test
    public void shouldTriggerBuildIfExclusionDisabledWithMatchingUser() throws IOException {
        SequentialExecutionQueue spiedQueue = addSpyToQueueField();

        String matchingUserName = "userName";
        FreeStyleProject project = jRule.createFreeStyleProject();
        GitHubPushTrigger trigger = new GitHubPushTrigger();
        trigger.setUseGitExcludedUsers(false);
        trigger.start(project, false);
        project.addTrigger(trigger);
        GitSCM scm = new GitSCM("https://localhost/dummy.git");
        UserExclusion userExclusion = new UserExclusion("something" + System.lineSeparator() +
                                                        matchingUserName + System.lineSeparator() +
                                                        "somethingElse" + System.lineSeparator()); 
        scm.getExtensions().add(userExclusion);
        project.setScm(scm);
        
        GitHubTriggerEvent event = GitHubTriggerEvent.create()
                                                     .withTimestamp(System.currentTimeMillis())
                                                     .withOrigin("origin")
                                                     .withTriggeredByUser(matchingUserName)
                                                     .build();
        trigger.onPost(event);
        
        verify(spiedQueue).execute(Mockito.any(Runnable.class));
    }

    @Test
    public void shouldTriggerBuildIfExclusionDisabledWithNonMatchingUser() throws IOException {
        SequentialExecutionQueue spiedQueue = addSpyToQueueField();
        
        FreeStyleProject project = jRule.createFreeStyleProject();
        GitHubPushTrigger trigger = new GitHubPushTrigger();
        trigger.setUseGitExcludedUsers(false);
        trigger.start(project, false);
        project.addTrigger(trigger);
        GitSCM scm = new GitSCM("https://localhost/dummy.git");
        UserExclusion userExclusion = new UserExclusion("something" + System.lineSeparator() +
                                                        "nonMatchingUserName" + System.lineSeparator() +
                                                        "somethingElse" + System.lineSeparator()); 
        scm.getExtensions().add(userExclusion);
        project.setScm(scm);
        
        GitHubTriggerEvent event = GitHubTriggerEvent.create()
                                                     .withTimestamp(System.currentTimeMillis())
                                                     .withOrigin("origin")
                                                     .withTriggeredByUser("userName")
                                                     .build();
        trigger.onPost(event);
        
        verify(spiedQueue).execute(Mockito.any(Runnable.class));
    }
}

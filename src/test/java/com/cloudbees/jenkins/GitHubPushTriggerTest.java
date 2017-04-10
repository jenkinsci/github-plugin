package com.cloudbees.jenkins;

import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;
import hudson.util.FormValidation;
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

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.cloudbees.jenkins.GitHubWebHookFullTest.classpath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.webhook.subscriber.DefaultPushGHEventListenerTest.HEAD_COMMIT;
import static org.jenkinsci.plugins.github.webhook.subscriber.DefaultPushGHEventListenerTest.MASTER_BRANCH_REF;
import static org.jenkinsci.plugins.github.webhook.subscriber.DefaultPushGHEventListenerTest.TRIGGERED_BY_USER_FROM_RESOURCE;

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

        trigger.onPost(TRIGGERED_BY_USER_FROM_RESOURCE, MASTER_BRANCH_REF, HEAD_COMMIT);

        TimeUnit.SECONDS.sleep(job.getQuietPeriod());
        jRule.waitUntilNoActivity();

        assertThat("should be 2 build after hook", job.getLastBuild().getNumber(), is(2));

        // Validate that the we have added 2 actions of the correct types
        // Validate that the parameters we have added are correct
        GitHubParametersAction parametersAction = job.getLastBuild().getAction(GitHubParametersAction.class);
        assertThat("found GitHubParametersAction", parametersAction != null);
        assertThat("should be 2 parameters added", parametersAction.getParameters(), hasSize(2));
        assertThat("first parameter is ref with value " + MASTER_BRANCH_REF,
                  "ref".equalsIgnoreCase(parametersAction.getParameters().get(0).getName())
                  && MASTER_BRANCH_REF.equals(parametersAction.getParameters().get(0).getValue()));
        assertThat("second parameter is head with value " + HEAD_COMMIT,
                   "head".equalsIgnoreCase(parametersAction.getParameters().get(1).getName())
                   && HEAD_COMMIT.equals(parametersAction.getParameters().get(1).getValue()));
        CauseAction causeAction = job.getLastBuild().getAction(CauseAction.class);
        assertThat("found CauseAction", causeAction != null);
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
}

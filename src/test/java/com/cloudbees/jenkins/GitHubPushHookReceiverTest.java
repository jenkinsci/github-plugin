package com.cloudbees.jenkins;

import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.util.Build;
import hudson.plugins.git.util.BuildData;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Test case covering the code responsible to receive the push notification
 * from GitHub.
 */
public class GitHubPushHookReceiverTest extends Assert {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Inject
    GitHubWebHook receiver;

    @Test
    public void receivePushHookOnFreeStyle() throws IOException, InterruptedException, ExecutionException {
        j.jenkins.getInjector().injectMembers(this);
        FreeStyleProject job = j.jenkins.createProject(FreeStyleProject.class, "Test");
        GitSCM scm = new GitSCM(
            Collections.singletonList(new UserRemoteConfig("https://github.com/amuniz/github-plugin.git", null, null, "github.com")),
            Collections.singletonList(new BranchSpec("")),
            false, Collections.<SubmoduleConfig>emptyList(),
            null, null, null);

        GitHubPushTrigger trigger = new GitHubPushTrigger();
        trigger.start(job, false);
        job.addTrigger(trigger);
        job.setScm(scm);

        String payload = IOUtils.toString(getClass().getResourceAsStream("/com/cloudbees/jenkins/plugins/github/payload2.json"));
        receiver.processGitHubPayload(payload, GitHubPushTrigger.class);
        Run build = waitForBuild(1, job);
        assertTrue("Build was triggered but did not success: " + build.getLog(300), Result.SUCCESS.equals(build.getResult()));
    }

    @Test
    public void receivePushHookOnWorkflow() throws Exception {
        j.jenkins.getInjector().injectMembers(this);
        WorkflowJob job = j.jenkins.createProject(WorkflowJob.class, "Test Workflow");

        GitHubPushTrigger trigger = new GitHubPushTrigger();
        trigger.start(job, false);
        job.addTrigger(trigger);
        job.setDefinition(new CpsFlowDefinition(
            "node {\n" +
            "    git credentialsId: '', url: 'https://github.com/amuniz/github-plugin.git'\n" +
            "}"));

        // Trigger the build once to register SCMs
        WorkflowRun lastRun = j.assertBuildStatusSuccess(job.scheduleBuild2(0));
        // Testing hack! This will make the polling believe that there was remote changes to build
        lastRun.getActions(BuildData.class).get(0).buildsByBranchName = new HashMap<String, Build>();

        // Then simulate a GitHub push
        String payload = IOUtils.toString(getClass().getResourceAsStream("/com/cloudbees/jenkins/plugins/github/payload2.json"));
        receiver.processGitHubPayload(payload, GitHubPushTrigger.class);
        Run build = waitForBuild(2, job);
        assertTrue("Build was triggered but did not success: " + build.getLog(300), Result.SUCCESS.equals(build.getResult()));
    }

    private Run waitForBuild(int n, Job job) throws InterruptedException {
        System.out.println("Waiting for build #" + n + " to appear and finish");
        int counter = 0;
        while (true) {
            if(counter > 30) {
                assertTrue("Waiting more than a minute for the build to start", false);
            }
            Run b = job.getBuildByNumber(n);
            if (b != null && !b.isBuilding()) {
                return b;
            }
            Thread.sleep(2000);
            counter++;
        }
    }
}

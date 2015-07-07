package com.cloudbees.jenkins;

import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
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

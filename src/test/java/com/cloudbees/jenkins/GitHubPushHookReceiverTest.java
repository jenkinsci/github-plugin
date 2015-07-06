package com.cloudbees.jenkins;

import hudson.model.FreeStyleProject;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Test case covering the code responsible to receive the push notification
 * from GitHub.
 */
public class GitHubPushHookReceiverTest {

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

        job.addTrigger(new GitHubPushTrigger());
        job.setScm(scm);

        String payload = IOUtils.toString(getClass().getResourceAsStream("payload2.json"));
        receiver.processGitHubPayload(payload, GitHubPushTrigger.class);
    }
}

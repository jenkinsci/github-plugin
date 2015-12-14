/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cloudbees.jenkins;

import hudson.model.Build;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.git.GitSCM;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests for {@link GitHubCommitNotifier}.
 *
 * @author Oleg Nenashev <o.v.nenashev@gmail.com>
 */
public class GitHubCommitNotifierTest {

    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    @Test
    @Issue("JENKINS-23641")
    public void testNoBuildData() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject("23641_noBuildData");
        prj.getPublishersList().add(new GitHubCommitNotifier());
        Build b = prj.scheduleBuild2(0).get();
        jRule.assertBuildStatus(Result.FAILURE, b);
        jRule.assertLogContains(org.jenkinsci.plugins.github.util.Messages.BuildDataHelper_NoBuildDataError(), b);
    }

    @Test
    @Issue("JENKINS-23641")
    public void testNoBuildRevision() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject();
        prj.setScm(new GitSCM("http://non.existent.git.repo.nowhere/repo.git"));
        prj.getPublishersList().add(new GitHubCommitNotifier());
        Build b = prj.scheduleBuild2(0).get();
        jRule.assertBuildStatus(Result.FAILURE, b);
        jRule.assertLogContains(org.jenkinsci.plugins.github.util.Messages.BuildDataHelper_NoLastRevisionError(), b);
    }

    @Test
    @Issue("JENKINS-25312")
    public void testMarkUnstableOnCommitNotifierFailure() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject();
        prj.getPublishersList().add(new GitHubCommitNotifier(Result.UNSTABLE.toString()));
        Build b = prj.scheduleBuild2(0).get();
        jRule.assertBuildStatus(Result.UNSTABLE, b);
    }

    @Test
    @Issue("JENKINS-25312")
    public void testMarkSuccessOnCommitNotifierFailure() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject();
        prj.getPublishersList().add(new GitHubCommitNotifier(Result.SUCCESS.toString()));
        Build b = prj.scheduleBuild2(0).get();
        jRule.assertBuildStatus(Result.SUCCESS, b);
    }
}

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
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests for {@link GitHubCommitNotifier}.
 * @author Oleg Nenashev <o.v.nenashev@gmail.com>
 */
public class GitHubCommitNotifierTest extends HudsonTestCase {
    
  //  @Rule
  //  public JenkinsRule r = new JenkinsRule();
    
    
    @Test
    @Bug(23641)
    public void testNoBuildData() throws Exception, InterruptedException  {
        FreeStyleProject prj = createFreeStyleProject("23641_noBuildData");
        prj.getPublishersList().add(new GitHubCommitNotifier());
        Build b = prj.scheduleBuild2(0).get();
        assertBuildStatus(Result.FAILURE, b);
        assertLogContains(Messages.GitHubCommitNotifier_NoBuildDataError(), b);
    }
    
    @Test
    @Bug(23641)
    public void testNoBuildRevision() throws Exception, InterruptedException {
        FreeStyleProject prj = createFreeStyleProject();
        prj.setScm(new GitSCM("http://non.existent.git.repo.nowhere/repo.git"));
        prj.getPublishersList().add(new GitHubCommitNotifier());
        Build b = prj.scheduleBuild2(0).get();
        assertBuildStatus(Result.FAILURE, b);
        assertLogContains(Messages.GitHubCommitNotifier_NoLastRevisionError(), b);
    }
   
    @Bug(25312)
    public @Test void testMarkUnstableOnCommitNotifierFailure() throws Exception, InterruptedException {
        FreeStyleProject prj = createFreeStyleProject();
        prj.getPublishersList().add(new GitHubCommitNotifier(Result.UNSTABLE.toString()));
        Build b = prj.scheduleBuild2(0).get();
        assertBuildStatus(Result.UNSTABLE, b);
    }
    
    @Bug(25312)
    public @Test void testMarkSuccessOnCommitNotifierFailure() throws Exception, InterruptedException {
        FreeStyleProject prj = createFreeStyleProject();
        prj.getPublishersList().add(new GitHubCommitNotifier(Result.SUCCESS.toString()));
        Build b = prj.scheduleBuild2(0).get();
        assertBuildStatus(Result.SUCCESS, b);
    }
}

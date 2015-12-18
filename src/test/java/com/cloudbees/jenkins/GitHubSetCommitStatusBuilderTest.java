package com.cloudbees.jenkins;

import hudson.model.Build;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests for {@link GitHubSetCommitStatusBuilder}.
 *
 * @author Oleg Nenashev <o.v.nenashev@gmail.com>
 */
public class GitHubSetCommitStatusBuilderTest {

    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    @Test
    @Issue("JENKINS-23641")
    public void testNoBuildData() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject("23641_noBuildData");
        prj.getBuildersList().add(new GitHubSetCommitStatusBuilder());
        Build b = prj.scheduleBuild2(0).get();
        jRule.assertBuildStatus(Result.FAILURE, b);
        jRule.assertLogContains(org.jenkinsci.plugins.github.util.Messages.BuildDataHelper_NoBuildDataError(), b);
    }
}

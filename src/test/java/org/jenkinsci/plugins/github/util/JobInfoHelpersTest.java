package org.jenkinsci.plugins.github.util;

import com.cloudbees.jenkins.GitHubPushTrigger;
import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.withTrigger;
import static org.junit.Assert.assertThat;

/**
 * @author lanwen (Merkushev Kirill)
 *         Date: 03.07.15
 */
public class JobInfoHelpersTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void shouldMatchForProjectWithTrigger() throws Exception {
        FreeStyleProject prj = jenkins.createFreeStyleProject();
        prj.addTrigger(new GitHubPushTrigger());

        assertThat("with trigger", withTrigger(GitHubPushTrigger.class).apply(prj), is(true));
    }

    @Test
    public void shouldNotMatchProjectWithoutTrigger() throws Exception {
        FreeStyleProject prj = jenkins.createFreeStyleProject();

        assertThat("without trigger", withTrigger(GitHubPushTrigger.class).apply(prj), is(false));
    }
}

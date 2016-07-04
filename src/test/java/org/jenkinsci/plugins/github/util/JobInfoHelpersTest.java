package org.jenkinsci.plugins.github.util;

import com.cloudbees.jenkins.GitHubPushTrigger;
import hudson.model.FreeStyleProject;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isAlive;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isBuildable;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.triggerFrom;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.withTrigger;
import static org.junit.Assert.assertThat;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class JobInfoHelpersTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void shouldMatchForProjectWithTrigger() throws Exception {
        FreeStyleProject prj = jenkins.createFreeStyleProject();
        prj.addTrigger(new GitHubPushTrigger(null));

        assertThat("with trigger", withTrigger(GitHubPushTrigger.class).apply(prj), is(true));
    }

    @Test
    public void shouldSeeProjectWithTriggerIsAliveForCleaner() throws Exception {
        FreeStyleProject prj = jenkins.createFreeStyleProject();
        prj.addTrigger(new GitHubPushTrigger(null));

        assertThat("with trigger", isAlive().apply(prj), is(true));
    }

    @Test
    public void shouldNotMatchProjectWithoutTrigger() throws Exception {
        FreeStyleProject prj = jenkins.createFreeStyleProject();

        assertThat("without trigger", withTrigger(GitHubPushTrigger.class).apply(prj), is(false));
    }

    @Test
    public void shouldNotMatchNullProject() throws Exception {
        assertThat("null project", withTrigger(GitHubPushTrigger.class).apply(null), is(false));
    }

    @Test
    public void shouldReturnNotBuildableOnNullProject() throws Exception {
        assertThat("null project", isBuildable().apply(null), is(false));
    }

    @Test
    public void shouldSeeProjectWithoutTriggerIsNotAliveForCleaner() throws Exception {
        FreeStyleProject prj = jenkins.createFreeStyleProject();

        assertThat("without trigger", isAlive().apply(prj), is(false));
    }

    @Test
    public void shouldGetTriggerFromAbstractProject() throws Exception {
        GitHubPushTrigger trigger = new GitHubPushTrigger(null);

        FreeStyleProject prj = jenkins.createFreeStyleProject();
        prj.addTrigger(trigger);

        assertThat("with trigger in free style job", triggerFrom(prj, GitHubPushTrigger.class), is(trigger));
    }

    @Test
    public void shouldGetTriggerFromWorkflow() throws Exception {
        GitHubPushTrigger trigger = new GitHubPushTrigger(null);
        WorkflowJob job = jenkins.getInstance().createProject(WorkflowJob.class, "Test Workflow");
        job.addTrigger(trigger);

        assertThat("with trigger in workflow", triggerFrom(job, GitHubPushTrigger.class), is(trigger));
    }

    @Test
    public void shouldNotGetTriggerWhenNoOne() throws Exception {
        FreeStyleProject prj = jenkins.createFreeStyleProject();

        assertThat("without trigger in project", triggerFrom(prj, GitHubPushTrigger.class), nullValue());
    }
}

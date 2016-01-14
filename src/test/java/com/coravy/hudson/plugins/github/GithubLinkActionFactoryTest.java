package com.coravy.hudson.plugins.github;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collection;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.coravy.hudson.plugins.github.GithubLinkAction.GithubLinkActionFactory;

import hudson.model.Action;

public class GithubLinkActionFactoryTest {
    @Rule
    public JenkinsRule rule = new JenkinsRule();

    private GithubLinkActionFactory factory = new GithubLinkActionFactory();

    private WorkflowJob createExampleJob() throws IOException {
        return rule.getInstance().createProject(WorkflowJob.class, "example");
    }

    @Test
    public void shouldCreateGithubLinkActionForJobWithGithubProjectProperty() throws IOException {
        WorkflowJob job = createExampleJob();
        String url = "https://github.com/jenkinsci/github-plugin/";
        job.addProperty(new GithubProjectProperty(url));

        Collection<? extends Action> actions = factory.createFor(job);
        assertThat("Only one Action should have been created", actions.size(), is(1));

        Action action = actions.iterator().next();
        assertThat("Created Action should be instance of GithubLinkAction", action, is(instanceOf(GithubLinkAction.class)));
        assertThat("Action URL should equal url set in GithubProjectProperty", action.getUrlName(), is(url));
    }

    @Test
    public void shouldNotCreateGithubLinkActionForJobWithoutGithubProjectProperty() throws IOException {
        WorkflowJob job = createExampleJob();

        Collection<? extends Action> actions = factory.createFor(job);
        assertThat("No Action should have been created", actions, is(empty()));
    }
}

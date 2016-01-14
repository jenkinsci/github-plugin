package com.coravy.hudson.plugins.github;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
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
    public JenkinsRule j = new JenkinsRule();

    public GithubLinkActionFactory factory = new GithubLinkActionFactory();

    @Test
    public void shouldCreateGithubLinkActionForJobWithGithubProjectProperty() throws IOException {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.addProperty(new GithubProjectProperty("https://github.com/jenkinsci/github-plugin/"));

        Collection<? extends Action> actions = factory.createFor(p);
        assertThat(actions.size(), is(equalTo(1)));

        GithubLinkAction action = (GithubLinkAction) actions.iterator().next();
        assertThat(action.getUrlName(), is(equalTo("https://github.com/jenkinsci/github-plugin/")));
    }

    @Test
    public void shouldNotCreateGithubLinkActionForJobWithoutGithubProjectProperty() throws IOException {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");

        Collection<? extends Action> actions = factory.createFor(p);
        assertThat(actions, is(empty()));
    }
}

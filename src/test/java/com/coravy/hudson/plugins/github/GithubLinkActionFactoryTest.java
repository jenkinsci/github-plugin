package com.coravy.hudson.plugins.github;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.MatcherAssert.assertThat;

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
    public final JenkinsRule rule = new JenkinsRule();

    private final GithubLinkActionFactory factory = new GithubLinkActionFactory();

    private static final String PROJECT_URL = "https://github.com/jenkinsci/github-plugin/";

    private WorkflowJob createExampleJob() throws IOException {
        return rule.getInstance().createProject(WorkflowJob.class, "example");
    }

    private GithubProjectProperty createExampleProperty() {
        return new GithubProjectProperty(PROJECT_URL);
    }

    @Test
    public void shouldCreateGithubLinkActionForJobWithGithubProjectProperty() throws IOException {
        final WorkflowJob job = createExampleJob();
        final GithubProjectProperty property = createExampleProperty();
        job.addProperty(property);

        final Collection<? extends Action> actions = factory.createFor(job);
        assertThat("factored actions list", actions.size(), is(1));

        final Action action = actions.iterator().next();
        assertThat("instance check", action, is(instanceOf(GithubLinkAction.class)));
        assertThat("url of action", action.getUrlName(), is(property.getProjectUrlStr()));
    }

    @Test
    public void shouldNotCreateGithubLinkActionForJobWithoutGithubProjectProperty() throws IOException {
        final WorkflowJob job = createExampleJob();

        final Collection<? extends Action> actions = factory.createFor(job);
        assertThat("factored actions list", actions, is(empty()));
    }
}

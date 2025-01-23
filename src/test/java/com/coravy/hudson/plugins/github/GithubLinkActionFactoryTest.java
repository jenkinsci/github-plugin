package com.coravy.hudson.plugins.github;

import com.coravy.hudson.plugins.github.GithubLinkAction.GithubLinkActionFactory;
import hudson.model.Action;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@WithJenkins
class GithubLinkActionFactoryTest {
    private JenkinsRule rule;

    private final GithubLinkActionFactory factory = new GithubLinkActionFactory();

    private static final String PROJECT_URL = "https://github.com/jenkinsci/github-plugin/";

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        this.rule = rule;
    }

    private WorkflowJob createExampleJob() throws IOException {
        return rule.getInstance().createProject(WorkflowJob.class, "example");
    }

    private GithubProjectProperty createExampleProperty() {
        return new GithubProjectProperty(PROJECT_URL);
    }

    @Test
    void shouldCreateGithubLinkActionForJobWithGithubProjectProperty() throws IOException {
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
    void shouldNotCreateGithubLinkActionForJobWithoutGithubProjectProperty() throws IOException {
        final WorkflowJob job = createExampleJob();

        final Collection<? extends Action> actions = factory.createFor(job);
        assertThat("factored actions list", actions, is(empty()));
    }
}

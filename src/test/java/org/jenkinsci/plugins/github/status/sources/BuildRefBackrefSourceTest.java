package org.jenkinsci.plugins.github.status.sources;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author pupssman (Kalinin Ivan)
 */
@WithJenkins
@ExtendWith(MockitoExtension.class)
class BuildRefBackrefSourceTest {

    private JenkinsRule jenkinsRule;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private TaskListener listener;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkinsRule = rule;
    }

    /**
     * @throws Exception
     */
    @Test
    void shouldReturnRunAbsoluteUrl() throws Exception {
        Run<?, ?> run = jenkinsRule.buildAndAssertSuccess(jenkinsRule.createFreeStyleProject());

        String result = new BuildRefBackrefSource().get(run, listener);
        assertThat("state", result, is(DisplayURLProvider.get().getRunURL(run)));
    }

}

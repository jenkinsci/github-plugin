package org.jenkinsci.plugins.github.status.sources;

import hudson.model.FreeStyleProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author pupssman (Kalinin Ivan)
 */
@RunWith(MockitoJUnitRunner.class)
public class BuildRefBackrefSourceTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Mock(answer = Answers.RETURNS_MOCKS)
    private TaskListener listener;

    @Test
    /**
     * @throws Exception
     */
    public void shouldReturnRunAbsoluteUrl() throws Exception {
        Run<?, ?> run = jenkinsRule.buildAndAssertSuccess(jenkinsRule.createFreeStyleProject());

        String result = new BuildRefBackrefSource().get(run, listener);
        assertThat("state", result, is(DisplayURLProvider.get().getRunURL(run)));
    }

}

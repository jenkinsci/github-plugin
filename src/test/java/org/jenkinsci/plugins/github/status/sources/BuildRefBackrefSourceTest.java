package org.jenkinsci.plugins.github.status.sources;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * @author pupssman (Kalinin Ivan)
 */
@RunWith(MockitoJUnitRunner.class)
public class BuildRefBackrefSourceTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock(answer = Answers.RETURNS_MOCKS)
    private Run run;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private TaskListener listener;

    @Test
    public void shouldReturnRunAbsoluteUrl() throws Exception {
        when(run.getAbsoluteUrl()).thenReturn("ABSOLUTE_URL");

        String result = new BuildRefBackrefSource().get(run, listener);
        assertThat("state", result, is("ABSOLUTE_URL"));
    }

}

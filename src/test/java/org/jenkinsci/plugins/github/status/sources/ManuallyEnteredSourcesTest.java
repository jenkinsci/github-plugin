package org.jenkinsci.plugins.github.status.sources;

import hudson.EnvVars;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class ManuallyEnteredSourcesTest {

    public static final String EXPANDED = "expanded";
    @Mock(answer = Answers.RETURNS_MOCKS)
    private Run run;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private TaskListener listener;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private EnvVars env;


    @Test
    public void shouldExpandContext() throws Exception {
        when(run.getEnvironment(listener)).thenReturn(env);
        when(env.expand(Matchers.anyString())).thenReturn(EXPANDED);

        String context = new ManuallyEnteredCommitContextSource("").context(run, listener);
        assertThat(context, equalTo(EXPANDED));
    }

    @Test
    public void shouldExpandSha() throws Exception {
        when(run.getEnvironment(listener)).thenReturn(env);
        when(env.expand(Matchers.anyString())).thenReturn(EXPANDED);

        String context = new ManuallyEnteredShaSource("").get(run, listener);
        assertThat(context, equalTo(EXPANDED));
    }

    @Test
    public void shouldExpandBackref() throws Exception {
        when(run.getEnvironment(listener)).thenReturn(env);
        when(env.expand(Matchers.anyString())).thenReturn(EXPANDED);

        String context = new ManuallyEnteredBackrefSource("").get(run, listener);
        assertThat(context, equalTo(EXPANDED));
    }
}
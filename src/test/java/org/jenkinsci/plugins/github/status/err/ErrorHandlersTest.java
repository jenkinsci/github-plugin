package org.jenkinsci.plugins.github.status.err;

import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class ErrorHandlersTest {

    @Mock
    private Run run;

    @Mock
    private TaskListener listener;

    @Test
    public void shouldSetFailureResultStatus() throws Exception {
        boolean handled = new ChangingBuildStatusErrorHandler(Result.FAILURE.toString())
                .handle(new RuntimeException(), run, listener);

        verify(run).setResult(Result.FAILURE);
        assertThat("handling", handled, is(true));
    }

    @Test
    public void shouldSetFailureResultStatusOnUnknownSetup() throws Exception {
        boolean handled = new ChangingBuildStatusErrorHandler("")
                .handle(new RuntimeException(), run, listener);

        verify(run).setResult(Result.FAILURE);
        assertThat("handling", handled, is(true));
    }

    @Test
    public void shouldHandleAndDoNothing() throws Exception {
        boolean handled = new ShallowAnyErrorHandler().handle(new RuntimeException(), run, listener);
        assertThat("handling", handled, is(true));

        Mockito.verifyNoMoreInteractions(run);
    }
}
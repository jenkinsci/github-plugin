package org.jenkinsci.plugins.github.status.err;

import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

/**
 * @author lanwen (Merkushev Kirill)
 */
@ExtendWith(MockitoExtension.class)
class ErrorHandlersTest {

    @Mock
    private Run run;

    @Mock
    private TaskListener listener;

    @Test
    void shouldSetFailureResultStatus() throws Exception {
        boolean handled = new ChangingBuildStatusErrorHandler(Result.FAILURE.toString())
                .handle(new RuntimeException(), run, listener);

        verify(run).setResult(Result.FAILURE);
        assertThat("handling", handled, is(true));
    }

    @Test
    void shouldSetFailureResultStatusOnUnknownSetup() throws Exception {
        boolean handled = new ChangingBuildStatusErrorHandler("")
                .handle(new RuntimeException(), run, listener);

        verify(run).setResult(Result.FAILURE);
        assertThat("handling", handled, is(true));
    }

    @Test
    void shouldHandleAndDoNothing() throws Exception {
        boolean handled = new ShallowAnyErrorHandler().handle(new RuntimeException(), run, listener);
        assertThat("handling", handled, is(true));

        Mockito.verifyNoMoreInteractions(run);
    }
}
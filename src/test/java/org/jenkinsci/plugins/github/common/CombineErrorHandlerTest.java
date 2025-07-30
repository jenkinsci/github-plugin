package org.jenkinsci.plugins.github.common;

import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.status.err.ChangingBuildStatusErrorHandler;
import org.jenkinsci.plugins.github.status.err.ShallowAnyErrorHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.common.CombineErrorHandler.errorHandling;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author lanwen (Merkushev Kirill)
 */
@ExtendWith(MockitoExtension.class)
class CombineErrorHandlerTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    private Run run;

    @Mock
    private TaskListener listener;

    @Test
    void shouldRethrowExceptionIfNoMatch() {
        assertThrows(CombineErrorHandler.ErrorHandlingException.class, () ->

                errorHandling().handle(new RuntimeException(), run, listener));
    }

    @Test
    void shouldRethrowExceptionIfNullHandlersList() {
        assertThrows(CombineErrorHandler.ErrorHandlingException.class, () ->

                errorHandling().withHandlers(null).handle(new RuntimeException(), run, listener));
    }

    @Test
    void shouldHandleExceptionsWithHandler() throws Exception {
        boolean handled = errorHandling()
                .withHandlers(Collections.singletonList(new ShallowAnyErrorHandler()))
                .handle(new RuntimeException(), run, listener);

        assertThat("handling", handled, is(true));
    }

    @Test
    void shouldRethrowExceptionIfExceptionInside() {
        assertThrows(CombineErrorHandler.ErrorHandlingException.class, () ->

                errorHandling()
                        .withHandlers(Collections.singletonList(
                                (e, run, listener) -> {
                                throw new RuntimeException("wow");
                            }
                        ))
                        .handle(new RuntimeException(), run, listener));
    }

    @Test
    void shouldHandleExceptionWithFirstMatchAndSetStatus() throws Exception {
        boolean handled = errorHandling()
                .withHandlers(asList(
                        new ChangingBuildStatusErrorHandler(Result.FAILURE.toString()),
                        new ShallowAnyErrorHandler()
                ))
                .handle(new RuntimeException(), run, listener);

        assertThat("handling", handled, is(true));

        verify(run).setResult(Result.FAILURE);
        verify(run, times(2)).getParent();
        verifyNoMoreInteractions(run);
    }
}
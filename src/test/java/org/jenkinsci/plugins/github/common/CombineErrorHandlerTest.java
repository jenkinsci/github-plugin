package org.jenkinsci.plugins.github.common;

import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.status.err.ChangingBuildStatusErrorHandler;
import org.jenkinsci.plugins.github.status.err.ShallowAnyErrorHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import edu.umd.cs.findbugs.annotations.NonNull;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.common.CombineErrorHandler.errorHandling;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class CombineErrorHandlerTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    private Run run;

    @Mock
    private TaskListener listener;

    @Rule
    public ExpectedException exc = ExpectedException.none();

    @Test
    public void shouldRethrowExceptionIfNoMatch() throws Exception {
        exc.expect(CombineErrorHandler.ErrorHandlingException.class);

        errorHandling().handle(new RuntimeException(), run, listener);
    }

    @Test
    public void shouldRethrowExceptionIfNullHandlersList() throws Exception {
        exc.expect(CombineErrorHandler.ErrorHandlingException.class);

        errorHandling().withHandlers(null).handle(new RuntimeException(), run, listener);
    }

    @Test
    public void shouldHandleExceptionsWithHandler() throws Exception {
        boolean handled = errorHandling()
                .withHandlers(Collections.singletonList(new ShallowAnyErrorHandler()))
                .handle(new RuntimeException(), run, listener);

        assertThat("handling", handled, is(true));
    }

    @Test
    public void shouldRethrowExceptionIfExceptionInside() throws Exception {
        exc.expect(CombineErrorHandler.ErrorHandlingException.class);

        errorHandling()
                .withHandlers(Collections.singletonList(
                        new ErrorHandler() {
                            @Override
                            public boolean handle(Exception e, @NonNull Run<?, ?> run, @NonNull TaskListener listener) {
                                throw new RuntimeException("wow");
                            }
                        }
                ))
                .handle(new RuntimeException(), run, listener);
    }

    @Test
    public void shouldHandleExceptionWithFirstMatchAndSetStatus() throws Exception {
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
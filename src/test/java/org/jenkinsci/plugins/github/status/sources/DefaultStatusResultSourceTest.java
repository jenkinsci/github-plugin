package org.jenkinsci.plugins.github.status.sources;


import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusResultSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kohsuke.github.GHCommitState;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@ExtendWith(MockitoExtension.class)
class DefaultStatusResultSourceTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    private Run run;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private TaskListener listener;

    static Object[][] results() {
        return new Object[][]{
                {Result.SUCCESS, GHCommitState.SUCCESS},
                {Result.UNSTABLE, GHCommitState.FAILURE},
                {Result.FAILURE, GHCommitState.ERROR},
                {Result.ABORTED, GHCommitState.ERROR},
                {null, GHCommitState.PENDING},
        };
    }

    @ParameterizedTest
    @MethodSource("results")
    void shouldReturnConditionalResult(Result actual, GHCommitState expected) throws Exception {
        when(run.getResult()).thenReturn(actual);

        GitHubStatusResultSource.StatusResult result = new DefaultStatusResultSource().get(run, listener);
        assertThat("state", result.getState(), is(expected));
    }

}

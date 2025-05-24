package org.jenkinsci.plugins.github.status.sources;

import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusResultSource;
import org.jenkinsci.plugins.github.extension.status.misc.ConditionalResult;
import org.jenkinsci.plugins.github.status.sources.misc.AnyBuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHCommitState;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.jenkinsci.plugins.github.status.sources.misc.BetterThanOrEqualBuildResult.betterThanOrEqualTo;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@ExtendWith(MockitoExtension.class)
class ConditionalStatusResultSourceTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    private Run run;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private TaskListener listener;

    @Test
    void shouldReturnPendingByDefault() throws Exception {
        GitHubStatusResultSource.StatusResult res = new ConditionalStatusResultSource(null).get(run, listener);

        assertThat("state", res.getState(), is(GHCommitState.PENDING));
        assertThat("msg", res.getMsg(), notNullValue());
    }

    @Test
    void shouldReturnPendingIfNoMatch() throws Exception {
        when(run.getResult()).thenReturn(Result.FAILURE);

        GitHubStatusResultSource.StatusResult res = new ConditionalStatusResultSource(
                Collections.<ConditionalResult>singletonList(
                        betterThanOrEqualTo(Result.SUCCESS, GHCommitState.SUCCESS, "2")
                ))
                .get(run, listener);

        assertThat("state", res.getState(), is(GHCommitState.PENDING));
        assertThat("msg", res.getMsg(), notNullValue());
    }

    @Test
    void shouldReturnFirstMatch() throws Exception {
        GitHubStatusResultSource.StatusResult res = new ConditionalStatusResultSource(asList(
                AnyBuildResult.onAnyResult(GHCommitState.FAILURE, "1"),
                betterThanOrEqualTo(Result.SUCCESS, GHCommitState.SUCCESS, "2")
        )).get(run, listener);

        assertThat("state", res.getState(), is(GHCommitState.FAILURE));
        assertThat("msg", res.getMsg(), notNullValue());
    }

    @Test
    void shouldReturnFirstMatch2() throws Exception {
        when(run.getResult()).thenReturn(Result.SUCCESS);

        GitHubStatusResultSource.StatusResult res = new ConditionalStatusResultSource(asList(
                betterThanOrEqualTo(Result.SUCCESS, GHCommitState.SUCCESS, "2"),
                AnyBuildResult.onAnyResult(GHCommitState.FAILURE, "1")
        )).get(run, listener);

        assertThat("state", res.getState(), is(GHCommitState.SUCCESS));
        assertThat("msg", res.getMsg(), notNullValue());
    }
}
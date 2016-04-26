package org.jenkinsci.plugins.github.status.sources;

import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusResultSource;
import org.jenkinsci.plugins.github.extension.status.misc.ConditionalResult;
import org.jenkinsci.plugins.github.status.sources.misc.AnyBuildResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHCommitState;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.jenkinsci.plugins.github.status.sources.misc.BetterThanOrEqualBuildResult.betterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class ConditionalStatusResultSourceTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    private Run run;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private TaskListener listener;

    @Test
    public void shouldReturnPendingByDefault() throws Exception {
        GitHubStatusResultSource.StatusResult res = new ConditionalStatusResultSource(null).get(run, listener);

        assertThat("state", res.getState(), is(GHCommitState.PENDING));
        assertThat("msg", res.getMsg(), notNullValue());
    }

    @Test
    public void shouldReturnPendingIfNoMatch() throws Exception {
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
    public void shouldReturnFirstMatch() throws Exception {
        GitHubStatusResultSource.StatusResult res = new ConditionalStatusResultSource(asList(
                AnyBuildResult.onAnyResult(GHCommitState.FAILURE, "1"),
                betterThanOrEqualTo(Result.SUCCESS, GHCommitState.SUCCESS, "2")
        )).get(run, listener);

        assertThat("state", res.getState(), is(GHCommitState.FAILURE));
        assertThat("msg", res.getMsg(), notNullValue());
    }

    @Test
    public void shouldReturnFirstMatch2() throws Exception {
        when(run.getResult()).thenReturn(Result.SUCCESS);

        GitHubStatusResultSource.StatusResult res = new ConditionalStatusResultSource(asList(
                betterThanOrEqualTo(Result.SUCCESS, GHCommitState.SUCCESS, "2"),
                AnyBuildResult.onAnyResult(GHCommitState.FAILURE, "1")
        )).get(run, listener);

        assertThat("state", res.getState(), is(GHCommitState.SUCCESS));
        assertThat("msg", res.getMsg(), notNullValue());
    }
}
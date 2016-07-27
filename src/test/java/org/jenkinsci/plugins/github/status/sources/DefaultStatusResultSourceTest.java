package org.jenkinsci.plugins.github.status.sources;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusResultSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHCommitState;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(DataProviderRunner.class)
public class DefaultStatusResultSourceTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock(answer = Answers.RETURNS_MOCKS)
    private Run run;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private TaskListener listener;

    @DataProvider
    public static Object[][] results() {
        return new Object[][]{
                {Result.SUCCESS, GHCommitState.SUCCESS},
                {Result.UNSTABLE, GHCommitState.FAILURE},
                {Result.FAILURE, GHCommitState.ERROR},
                {Result.ABORTED, GHCommitState.ERROR},
                {null, GHCommitState.PENDING},
        };
    }

    @Test
    @UseDataProvider("results")
    public void shouldReturnConditionalResult(Result actual, GHCommitState expected) throws Exception {
        when(run.getResult()).thenReturn(actual);

        GitHubStatusResultSource.StatusResult result = new DefaultStatusResultSource().get(run, listener);
        assertThat("state", result.getState(), is(expected));
    }

}

package org.jenkinsci.plugins.github.status.sources.misc;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import hudson.model.Result;
import hudson.model.Run;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHCommitState;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.status.sources.misc.BetterThanOrEqualBuildResult.betterThanOrEqualTo;
import static org.junit.Assert.assertThat;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(DataProviderRunner.class)
public class BetterThanOrEqualBuildResultTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Run run;

    @DataProvider
    public static Object[][] results() {
        return new Object[][]{
                {Result.SUCCESS, Result.SUCCESS, true},
                {Result.UNSTABLE, Result.UNSTABLE, true},
                {Result.FAILURE, Result.FAILURE, true},
                {Result.FAILURE, Result.UNSTABLE, true},
                {Result.FAILURE, Result.SUCCESS, true},
                {Result.SUCCESS, Result.FAILURE, false},
                {Result.SUCCESS, Result.UNSTABLE, false},
                {Result.UNSTABLE, Result.FAILURE, false},
        };
    }

    @Test
    @UseDataProvider("results")
    public void shouldMatch(Result defined, Result real, boolean expect) throws Exception {
        Mockito.when(run.getResult()).thenReturn(real);

        boolean matched = betterThanOrEqualTo(defined, GHCommitState.FAILURE, "").matches(run);
        assertThat("matching", matched, is(expect));
    }
}
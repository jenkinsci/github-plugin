package org.jenkinsci.plugins.github.status.sources.misc;

import hudson.model.Result;
import hudson.model.Run;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kohsuke.github.GHCommitState;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.status.sources.misc.BetterThanOrEqualBuildResult.betterThanOrEqualTo;

/**
 * @author lanwen (Merkushev Kirill)
 */
@ExtendWith(MockitoExtension.class)
class BetterThanOrEqualBuildResultTest {

    @Mock
    private Run run;

    static Object[][] results() {
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

    @ParameterizedTest
    @MethodSource("results")
    void shouldMatch(Result defined, Result real, boolean expect) throws Exception {
        Mockito.when(run.getResult()).thenReturn(real);

        boolean matched = betterThanOrEqualTo(defined, GHCommitState.FAILURE, "").matches(run);
        assertThat("matching", matched, is(expect));
    }
}
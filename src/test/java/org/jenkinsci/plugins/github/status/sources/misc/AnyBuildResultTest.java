package org.jenkinsci.plugins.github.status.sources.misc;

import hudson.model.Run;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHCommitState;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author lanwen (Merkushev Kirill)
 */
@ExtendWith(MockitoExtension.class)
class AnyBuildResultTest {

    @Mock
    private Run run;

    @Test
    void shouldMatchEveryTime() throws Exception {
        boolean matches = AnyBuildResult.onAnyResult(GHCommitState.ERROR, "").matches(run);

        assertTrue(matches, "matching");
        verifyNoMoreInteractions(run);
    }

}
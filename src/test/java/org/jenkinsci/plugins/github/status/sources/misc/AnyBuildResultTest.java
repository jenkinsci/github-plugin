package org.jenkinsci.plugins.github.status.sources.misc;

import hudson.model.Run;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHCommitState;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class AnyBuildResultTest {

    @Mock
    private Run run;

    @Test
    public void shouldMatchEveryTime() throws Exception {
        boolean matches = AnyBuildResult.onAnyResult(GHCommitState.ERROR, "").matches(run);
        
        assertTrue("matching", matches);
        verifyNoMoreInteractions(run);
    }

}
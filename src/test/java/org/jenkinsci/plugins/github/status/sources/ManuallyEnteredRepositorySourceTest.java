package org.jenkinsci.plugins.github.status.sources;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.GHRepository;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintStream;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManuallyEnteredRepositorySourceTest {
    @Mock(answer = Answers.RETURNS_MOCKS)
    private Run run;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private TaskListener listener;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private PrintStream logger;

    @Test
    void nullName() {
        ManuallyEnteredRepositorySource instance = spy(new ManuallyEnteredRepositorySource("a"));
        doReturn(logger).when(listener).getLogger();
        List<GHRepository> repos = instance.repos(run, listener);
        assertThat("size", repos, hasSize(0));
        verify(listener).getLogger();
        verify(logger).printf(eq("Unable to match %s with a GitHub repository.%n"), eq("a"));
    }
}

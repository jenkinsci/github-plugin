package org.jenkinsci.plugins.github.status.sources;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHRepository;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.PrintStream;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ManuallyEnteredRepositorySourceTest {
    @Mock(answer = Answers.RETURNS_MOCKS)
    private Run run;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private TaskListener listener;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private PrintStream logger;

    @Test
    public void nullName() {
        ManuallyEnteredRepositorySource instance = spy(new ManuallyEnteredRepositorySource("a"));
        doReturn(logger).when(listener).getLogger();
        List<GHRepository> repos = instance.repos(run, listener);
        assertThat("size", repos, hasSize(0));
        verify(listener).getLogger();
        verify(logger).printf(eq("Unable to match %s with a GitHub repository.%n"), eq("a"));
    }
}

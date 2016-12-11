package org.jenkinsci.plugins.github.status.sources;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHRepository;
import org.mockito.Answers;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.PrintStream;
import java.util.List;

import static com.jayway.restassured.RestAssured.when;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
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
        ManuallyEnteredRepositorySource instance = Mockito.spy(new ManuallyEnteredRepositorySource("https://github.com/jenkinsci/jenkins"));
        doReturn(null).when(instance).createName(Matchers.anyString());
        doReturn(logger).when(listener).getLogger();
        List<GHRepository> repos = instance.repos(run, listener);
        assertThat("size", repos, hasSize(0));
        verify(listener).getLogger();
        verify(logger).printf(eq("Unable to match %s with a GitHub repository.%n"), eq("https://github.com/jenkinsci/jenkins"));
    }
}

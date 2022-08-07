package org.jenkinsci.plugins.github.admin;

import com.cloudbees.jenkins.GitHubRepositoryName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.Function;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.InvocationTargetException;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class ValidateRepoNameTest {
    public static final Object ANY_INSTANCE = null;
    public static final GitHubRepositoryName VALID_REPO = new GitHubRepositoryName("", "", "");

    @Mock
    private Function target;

    @Mock
    private StaplerRequest req;

    @Mock
    private StaplerResponse resp;

    @Rule
    public ExpectedException exc = ExpectedException.none();

    @Test
    public void shouldThrowInvocationExcOnNullsInArgs() throws Exception {
        ValidateRepoName.Processor processor = new ValidateRepoName.Processor();
        processor.setTarget(target);

        exc.expect(InvocationTargetException.class);

        processor.invoke(req, resp, ANY_INSTANCE, new Object[]{null});
    }

    @Test
    public void shouldNotThrowInvocationExcNameInArgs() throws Exception {
        ValidateRepoName.Processor processor = new ValidateRepoName.Processor();
        processor.setTarget(target);

        processor.invoke(req, resp, ANY_INSTANCE, new Object[]{VALID_REPO});
    }
}

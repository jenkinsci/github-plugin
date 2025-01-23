package org.jenkinsci.plugins.github.admin;

import com.cloudbees.jenkins.GitHubRepositoryName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.stapler.Function;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author lanwen (Merkushev Kirill)
 */
@ExtendWith(MockitoExtension.class)
class ValidateRepoNameTest {
    public static final Object ANY_INSTANCE = null;
    public static final GitHubRepositoryName VALID_REPO = new GitHubRepositoryName("", "", "");

    @Mock
    private Function target;

    @Mock
    private StaplerRequest2 req;

    @Mock
    private StaplerResponse2 resp;

    @Test
    void shouldThrowInvocationExcOnNullsInArgs() {
        assertThrows(InvocationTargetException.class, () -> {
            ValidateRepoName.Processor processor = new ValidateRepoName.Processor();
            processor.setTarget(target);

            processor.invoke(req, resp, ANY_INSTANCE, new Object[]{null});
        });
    }

    @Test
    void shouldNotThrowInvocationExcNameInArgs() throws Exception {
        ValidateRepoName.Processor processor = new ValidateRepoName.Processor();
        processor.setTarget(target);

        processor.invoke(req, resp, ANY_INSTANCE, new Object[]{VALID_REPO});
    }
}

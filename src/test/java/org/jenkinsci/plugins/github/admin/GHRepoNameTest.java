package org.jenkinsci.plugins.github.admin;

import com.cloudbees.jenkins.GitHubRepositoryName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class GHRepoNameTest {

    public static final String REPO_NAME_PARAMETER = "repo";
    private static final String REPO = "https://github.com/user/repo";

    @Mock
    private StaplerRequest req;

    @Mock
    private GHRepoName anno;

    @Test
    public void shouldExtractRepoNameFromForm() throws Exception {
        when(req.getParameter(REPO_NAME_PARAMETER)).thenReturn(REPO);
        GitHubRepositoryName repo = new GHRepoName.PayloadHandler().parse(req, anno, null, REPO_NAME_PARAMETER);

        assertThat("should parse repo", repo, is(GitHubRepositoryName.create(REPO)));
    }

    @Test
    public void shouldReturnNullOnNoAnyParam() throws Exception {
        GitHubRepositoryName repo = new GHRepoName.PayloadHandler().parse(req, anno, null, REPO_NAME_PARAMETER);

        assertThat("should not parse repo", repo, nullValue());
    }
}

package org.jenkinsci.plugins.github.admin;

import com.cloudbees.jenkins.GitHubRepositoryName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.stapler.StaplerRequest2;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@ExtendWith(MockitoExtension.class)
class GHRepoNameTest {

    public static final String REPO_NAME_PARAMETER = "repo";
    private static final String REPO = "https://github.com/user/repo";

    @Mock
    private StaplerRequest2 req;

    @Mock
    private GHRepoName anno;

    @Test
    void shouldExtractRepoNameFromForm() throws Exception {
        when(req.getParameter(REPO_NAME_PARAMETER)).thenReturn(REPO);
        GitHubRepositoryName repo = new GHRepoName.PayloadHandler().parse(req, anno, null, REPO_NAME_PARAMETER);

        assertThat("should parse repo", repo, is(GitHubRepositoryName.create(REPO)));
    }

    @Test
    void shouldReturnNullOnNoAnyParam() throws Exception {
        GitHubRepositoryName repo = new GHRepoName.PayloadHandler().parse(req, anno, null, REPO_NAME_PARAMETER);

        assertThat("should not parse repo", repo, nullValue());
    }
}

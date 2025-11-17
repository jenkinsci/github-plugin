package com.coravy.hudson.plugins.github;

import com.cloudbees.jenkins.GitHubRepositoryName;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.cloudbees.jenkins.GitHubRepositoryName.create;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.jenkinsci.plugins.github.test.GitHubRepoNameMatchers.repo;
import static org.jenkinsci.plugins.github.test.GitHubRepoNameMatchers.withHost;
import static org.jenkinsci.plugins.github.test.GitHubRepoNameMatchers.withRepoName;
import static org.jenkinsci.plugins.github.test.GitHubRepoNameMatchers.withUserName;

/**
 * Unit tests of {@link GitHubRepositoryName}
 */
public class GitHubRepositoryNameTest {

    public static final String FULL_REPO_NAME = "jenkins/jenkins";
    public static final String VALID_HTTPS_GH_PROJECT = "https://github.com/" + FULL_REPO_NAME;

    public static Object[][] repos() {
        return new Object[][]{
                new Object[]{"git@github.com:jenkinsci/jenkins.git/", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"git@github.com:jenkinsci/jenkins.git", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"git@github.com:jenkinsci/jenkins/", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"git@github.com:jenkinsci/jenkins", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"org-12345@github.com:jenkinsci/jenkins.git/", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"org-12345@github.com:jenkinsci/jenkins.git", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"org-12345@github.com:jenkinsci/jenkins/", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"org-12345@github.com:jenkinsci/jenkins", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"org-12345@gh.company.com:jenkinsci/jenkins.git/", "gh.company.com", "jenkinsci", "jenkins"},
                new Object[]{"git@gh.company.com:jenkinsci/jenkins.git", "gh.company.com", "jenkinsci", "jenkins"},
                new Object[]{"git@gh.company.com:jenkinsci/jenkins", "gh.company.com", "jenkinsci", "jenkins"},
                new Object[]{"git@gh.company.com:jenkinsci/jenkins/", "gh.company.com", "jenkinsci", "jenkins"},
                new Object[]{"git://github.com/jenkinsci/jenkins.git/", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"git://github.com/jenkinsci/jenkins.git", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"git://github.com/jenkinsci/jenkins/", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"git://github.com/jenkinsci/jenkins", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"https://user@github.com/jenkinsci/jenkins.git", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"https://user@github.com/jenkinsci/jenkins.git/", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"https://user@github.com/jenkinsci/jenkins", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"https://user@github.com/jenkinsci/jenkins/", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"https://employee@gh.company.com/jenkinsci/jenkins.git/", "gh.company.com", "jenkinsci", "jenkins"},
                new Object[]{"https://employee@gh.company.com/jenkinsci/jenkins.git", "gh.company.com", "jenkinsci", "jenkins"},
                new Object[]{"https://employee@gh.company.com/jenkinsci/jenkins", "gh.company.com", "jenkinsci", "jenkins"},
                new Object[]{"https://employee@gh.company.com/jenkinsci/jenkins/", "gh.company.com", "jenkinsci", "jenkins"},
                new Object[]{"git://company.net/jenkinsci/jenkins.git/", "company.net", "jenkinsci", "jenkins"},
                new Object[]{"git://company.net/jenkinsci/jenkins.git", "company.net", "jenkinsci", "jenkins"},
                new Object[]{"git://company.net/jenkinsci/jenkins", "company.net", "jenkinsci", "jenkins"},
                new Object[]{"git://company.net/jenkinsci/jenkins/", "company.net", "jenkinsci", "jenkins"},
                new Object[]{"https://github.com/jenkinsci/jenkins.git/", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"https://github.com/jenkinsci/jenkins.git", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"https://github.com/jenkinsci/jenkins", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"https://github.com/jenkinsci/jenkins/", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"ssh://git@github.com/jenkinsci/jenkins.git/", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"ssh://git@github.com/jenkinsci/jenkins.git", "github.com", "jenkinsci", "jenkins"},
                new Object[]{"ssh://git@github.com/jenkinsci/jenkins", "github.com", "jenkinsci", "jenkins",
                        new Object[]{"ssh://git@github.com/jenkinsci/jenkins/", "github.com", "jenkinsci", "jenkins",
                                new Object[]{"ssh://org-12345@github.com/jenkinsci/jenkins.git/", "github.com", "jenkinsci", "jenkins"},
                                new Object[]{"ssh://org-12345@github.com/jenkinsci/jenkins.git", "github.com", "jenkinsci", "jenkins"},
                                new Object[]{"ssh://org-12345@github.com/jenkinsci/jenkins", "github.com", "jenkinsci", "jenkins"},
                                new Object[]{"ssh://org-12345@github.com/jenkinsci/jenkins/", "github.com", "jenkinsci", "jenkins"},
                                new Object[]{"ssh://github.com/jenkinscRi/jenkins.git/", "github.com", "jenkinsci", "jenkins"},
                                new Object[]{"ssh://github.com/jenkinsci/jenkins.git", "github.com", "jenkinsci", "jenkins"},
                                new Object[]{"ssh://github.com/jenkinsci/jenkins", "github.com", "jenkinsci", "jenkins"},
                                new Object[]{"ssh://github.com/jenkinsci/jenkins/", "github.com", "jenkinsci", "jenkins"},
                                new Object[]{"git+ssh://git@github.com/jenkinsci/jenkins.git", "github.com", "jenkinsci", "jenkins"},
                                new Object[]{"git+ssh://org-12345@github.com/jenkinsci/jenkins.git", "github.com", "jenkinsci", "jenkins"},
                                new Object[]{"git+ssh://github.com/jenkinsci/jenkins", "github.com", "jenkinsci", "jenkins"}
                        }
                }
        };
    }

    @ParameterizedTest
    @MethodSource("repos")
    void githubFullRepo(String url, String host, String user, String repo) {
        assertThat(url, repo(allOf(
                withHost(host),
                withUserName(user),
                withRepoName(repo)
        )));
    }

    @Test
    void trimWhitespace() {
        assertThat("               https://user@github.com/jenkinsci/jenkins/      ", repo(allOf(
                withHost("github.com"),
                withUserName("jenkinsci"),
                withRepoName("jenkins")
        )));
    }

    @ParameterizedTest
    @ValueSource(strings = {"gopher://gopher.floodgap.com",
            "https//github.com/jenkinsci/jenkins",
            ""})
    @NullSource
    void badUrl(String url) {
        assertThat(url, repo(nullValue(GitHubRepositoryName.class)));
    }

    @Test
    void shouldCreateFromProjectProp() {
        assertThat("project prop vs direct", create(new GithubProjectProperty(VALID_HTTPS_GH_PROJECT)),
                equalTo(create(VALID_HTTPS_GH_PROJECT)));
    }

    @Test
    void shouldIgnoreNull() {
        assertThat("null project prop", create((GithubProjectProperty) null), nullValue());
    }

    @Test
    void shouldIgnoreNullValueOfPP() {
        assertThat("null project prop", create(new GithubProjectProperty(null)), nullValue());
    }

    @Test
    void shouldIgnoreBadValueOfPP() {
        assertThat("null project prop", create(new GithubProjectProperty(StringUtils.EMPTY)), nullValue());
    }
}

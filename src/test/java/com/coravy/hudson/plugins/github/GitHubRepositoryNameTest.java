package com.coravy.hudson.plugins.github;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.cloudbees.jenkins.GitHubRepositoryName.create;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.jenkinsci.plugins.github.test.GitHubRepoNameMatchers.repo;
import static org.jenkinsci.plugins.github.test.GitHubRepoNameMatchers.withHost;
import static org.jenkinsci.plugins.github.test.GitHubRepoNameMatchers.withRepoName;
import static org.jenkinsci.plugins.github.test.GitHubRepoNameMatchers.withUserName;
import static org.junit.Assert.assertThat;

/**
 * Unit tests of {@link GitHubRepositoryName}
 */
@RunWith(DataProviderRunner.class)
public class GitHubRepositoryNameTest {

    public static final String FULL_REPO_NAME = "jenkins/jenkins";
    public static final String VALID_HTTPS_GH_PROJECT = "https://github.com/" + FULL_REPO_NAME;

    @Test
    @DataProvider({
            "git@github.com:jenkinsci/jenkins.git/, github.com, jenkinsci, jenkins",
            "git@github.com:jenkinsci/jenkins.git, github.com, jenkinsci, jenkins",
            "git@github.com:jenkinsci/jenkins/, github.com, jenkinsci, jenkins",
            "git@github.com:jenkinsci/jenkins, github.com, jenkinsci, jenkins",
            "org-12345@github.com:jenkinsci/jenkins.git/, github.com, jenkinsci, jenkins",
            "org-12345@github.com:jenkinsci/jenkins.git, github.com, jenkinsci, jenkins",
            "org-12345@github.com:jenkinsci/jenkins/, github.com, jenkinsci, jenkins",
            "org-12345@github.com:jenkinsci/jenkins, github.com, jenkinsci, jenkins",
            "org-12345@gh.company.com:jenkinsci/jenkins.git/, gh.company.com, jenkinsci, jenkins",
            "git@gh.company.com:jenkinsci/jenkins.git, gh.company.com, jenkinsci, jenkins",
            "git@gh.company.com:jenkinsci/jenkins, gh.company.com, jenkinsci, jenkins",
            "git@gh.company.com:jenkinsci/jenkins/, gh.company.com, jenkinsci, jenkins",
            "git://github.com/jenkinsci/jenkins.git/, github.com, jenkinsci, jenkins",
            "git://github.com/jenkinsci/jenkins.git, github.com, jenkinsci, jenkins",
            "git://github.com/jenkinsci/jenkins/, github.com, jenkinsci, jenkins",
            "git://github.com/jenkinsci/jenkins, github.com, jenkinsci, jenkins",
            "https://user@github.com/jenkinsci/jenkins.git, github.com, jenkinsci, jenkins",
            "https://user@github.com/jenkinsci/jenkins.git/, github.com, jenkinsci, jenkins",
            "https://user@github.com/jenkinsci/jenkins, github.com, jenkinsci, jenkins",
            "https://user@github.com/jenkinsci/jenkins/, github.com, jenkinsci, jenkins",
            "https://employee@gh.company.com/jenkinsci/jenkins.git/, gh.company.com, jenkinsci, jenkins",
            "https://employee@gh.company.com/jenkinsci/jenkins.git, gh.company.com, jenkinsci, jenkins",
            "https://employee@gh.company.com/jenkinsci/jenkins, gh.company.com, jenkinsci, jenkins",
            "https://employee@gh.company.com/jenkinsci/jenkins/, gh.company.com, jenkinsci, jenkins",
            "git://company.net/jenkinsci/jenkins.git/, company.net, jenkinsci, jenkins",
            "git://company.net/jenkinsci/jenkins.git, company.net, jenkinsci, jenkins",
            "git://company.net/jenkinsci/jenkins, company.net, jenkinsci, jenkins",
            "git://company.net/jenkinsci/jenkins/, company.net, jenkinsci, jenkins",
            "https://github.com/jenkinsci/jenkins.git/, github.com, jenkinsci, jenkins",
            "https://github.com/jenkinsci/jenkins.git, github.com, jenkinsci, jenkins",
            "https://github.com/jenkinsci/jenkins, github.com, jenkinsci, jenkins",
            "https://github.com/jenkinsci/jenkins/, github.com, jenkinsci, jenkins",
            "ssh://git@github.com/jenkinsci/jenkins.git/, github.com, jenkinsci, jenkins",
            "ssh://git@github.com/jenkinsci/jenkins.git, github.com, jenkinsci, jenkins",
            "ssh://git@github.com/jenkinsci/jenkins, github.com, jenkinsci, jenkins",
            "ssh://git@github.com/jenkinsci/jenkins/, github.com, jenkinsci, jenkins",
            "ssh://org-12345@github.com/jenkinsci/jenkins.git/, github.com, jenkinsci, jenkins",
            "ssh://org-12345@github.com/jenkinsci/jenkins.git, github.com, jenkinsci, jenkins",
            "ssh://org-12345@github.com/jenkinsci/jenkins, github.com, jenkinsci, jenkins",
            "ssh://org-12345@github.com/jenkinsci/jenkins/, github.com, jenkinsci, jenkins",
            "ssh://github.com/jenkinsci/jenkins.git/, github.com, jenkinsci, jenkins",
            "ssh://github.com/jenkinsci/jenkins.git, github.com, jenkinsci, jenkins",
            "ssh://github.com/jenkinsci/jenkins, github.com, jenkinsci, jenkins",
            "ssh://github.com/jenkinsci/jenkins/, github.com, jenkinsci, jenkins",
            "git+ssh://git@github.com/jenkinsci/jenkins.git, github.com, jenkinsci, jenkins",
            "git+ssh://org-12345@github.com/jenkinsci/jenkins.git, github.com, jenkinsci, jenkins",
            "git+ssh://github.com/jenkinsci/jenkins, github.com, jenkinsci, jenkins",
    })
    public void githubFullRepo(String url, String host, String user, String repo) {
        assertThat(url, repo(allOf(
                withHost(host),
                withUserName(user),
                withRepoName(repo)
        )));
    }

    @Test
    public void trimWhitespace() {
        assertThat("               https://user@github.com/jenkinsci/jenkins/      ", repo(allOf(
                withHost("github.com"),
                withUserName("jenkinsci"),
                withRepoName("jenkins")
        )));
    }

    @Test
    @DataProvider(value = {
            "gopher://gopher.floodgap.com",
            "https//github.com/jenkinsci/jenkins",
            "",
            "null"
    }, trimValues = false)
    public void badUrl(String url) {
        assertThat(url, repo(nullValue(GitHubRepositoryName.class)));
    }

    @Test
    public void shouldCreateFromProjectProp() {
        assertThat("project prop vs direct", create(new GithubProjectProperty(VALID_HTTPS_GH_PROJECT)),
                equalTo(create(VALID_HTTPS_GH_PROJECT)));
    }

    @Test
    public void shouldIgnoreNull() {
        assertThat("null project prop", create((GithubProjectProperty) null), nullValue());
    }

    @Test
    public void shouldIgnoreNullValueOfPP() {
        assertThat("null project prop", create(new GithubProjectProperty(null)), nullValue());
    }

    @Test
    public void shouldIgnoreBadValueOfPP() {
        assertThat("null project prop", create(new GithubProjectProperty(StringUtils.EMPTY)), nullValue());
    }
}

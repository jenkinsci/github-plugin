package com.coravy.hudson.plugins.github;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.allOf;
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

    @Test
    @DataProvider({
            "git@github.com:jenkinsci/jenkins.git, github.com, jenkinsci, jenkins",
            "git@github.com:jenkinsci/jenkins/, github.com, jenkinsci, jenkins",
            "git@github.com:jenkinsci/jenkins, github.com, jenkinsci, jenkins",
            "git@gh.company.com:jenkinsci/jenkins.git, gh.company.com, jenkinsci, jenkins",
            "git@gh.company.com:jenkinsci/jenkins, gh.company.com, jenkinsci, jenkins",
            "git@gh.company.com:jenkinsci/jenkins/, gh.company.com, jenkinsci, jenkins",
            "git://github.com/jenkinsci/jenkins.git, github.com, jenkinsci, jenkins",
            "git://github.com/jenkinsci/jenkins/, github.com, jenkinsci, jenkins",
            "git://github.com/jenkinsci/jenkins, github.com, jenkinsci, jenkins",
            "https://user@github.com/jenkinsci/jenkins.git, github.com, jenkinsci, jenkins",
            "https://user@github.com/jenkinsci/jenkins, github.com, jenkinsci, jenkins",
            "https://user@github.com/jenkinsci/jenkins/, github.com, jenkinsci, jenkins",
            "https://employee@gh.company.com/jenkinsci/jenkins.git, gh.company.com, jenkinsci, jenkins",
            "https://employee@gh.company.com/jenkinsci/jenkins, gh.company.com, jenkinsci, jenkins",
            "https://employee@gh.company.com/jenkinsci/jenkins/, gh.company.com, jenkinsci, jenkins",
            "git://company.net/jenkinsci/jenkins.git, company.net, jenkinsci, jenkins",
            "git://company.net/jenkinsci/jenkins, company.net, jenkinsci, jenkins",
            "git://company.net/jenkinsci/jenkins/, company.net, jenkinsci, jenkins",
            "https://github.com/jenkinsci/jenkins.git, github.com, jenkinsci, jenkins",
            "https://github.com/jenkinsci/jenkins, github.com, jenkinsci, jenkins",
            "https://github.com/jenkinsci/jenkins/, github.com, jenkinsci, jenkins",
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
}

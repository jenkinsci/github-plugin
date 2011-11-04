package com.coravy.hudson.plugins.github;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.cloudbees.jenkins.GitHubRepositoryName;

import org.junit.Test;

/**
 * Unit tests of {@link GitHubRepositoryName}
 */
public class GitHubRepositoryNameTest {

    @Test
    public void gitAtUrlGitHub() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("git@github.com:jenkinsci/jenkins.git");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("github.com", repo.host);
    }

    @Test
    public void gitAtUrlOtherHost() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("git@gh.company.com:jenkinsci/jenkins.git");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("gh.company.com", repo.host);
    }

    @Test
    public void gitColonUrlGitHub() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("git://github.com/jenkinsci/jenkins.git");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("github.com", repo.host);
    }

    @Test
    public void gitColonUrlOtherHost() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("git://company.net/jenkinsci/jenkins.git");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("company.net", repo.host);
    }

    @Test
    public void httpsUrlGitHub() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("https://user@github.com/jenkinsci/jenkins.git");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("github.com", repo.host);
    }

    @Test
    public void httpsUrlOtherHost() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("https://employee@gh.company.com/jenkinsci/jenkins.git");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("gh.company.com", repo.host);
    }
}

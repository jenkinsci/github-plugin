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
    public void httpsUrlGitHubWithoutUser() {
        //this is valid for anonymous usage
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("https://github.com/jenkinsci/jenkins.git");
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

    @Test
    public void gitAtUrlGitHubNoSuffix() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("git@github.com:jenkinsci/jenkins");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("github.com", repo.host);
    }

    @Test
    public void gitAtUrlOtherHostNoSuffix() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("git@gh.company.com:jenkinsci/jenkins");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("gh.company.com", repo.host);
    }

    @Test
    public void gitColonUrlGitHubNoSuffix() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("git://github.com/jenkinsci/jenkins");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("github.com", repo.host);
    }

    @Test
    public void gitColonUrlOtherHostNoSuffix() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("git://company.net/jenkinsci/jenkins");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("company.net", repo.host);
    }

    @Test
    public void httpsUrlGitHubNoSuffix() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("https://user@github.com/jenkinsci/jenkins");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("github.com", repo.host);
    }
    
    @Test
    public void httpsUrlGitHubWithoutUserNoSuffix() {
        //this is valid for anonymous usage
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("https://github.com/jenkinsci/jenkins");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("github.com", repo.host);
    }

    @Test
    public void httpsUrlOtherHostNoSuffix() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("https://employee@gh.company.com/jenkinsci/jenkins");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("gh.company.com", repo.host);
    }
}

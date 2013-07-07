package com.coravy.hudson.plugins.github;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.cloudbees.jenkins.GitHubRepositoryName;

import org.junit.Test;

/**
 * Unit tests of {@link GitHubRepositoryName}
 */
public class GitHubRepositoryNameTest {

    private void testURL(String URL, String host, String owner, String repository)
    {
        GitHubRepositoryName repo = GitHubRepositoryName.create(URL);
        assertNotNull(repo);
        assertEquals(host, repo.host);
        assertEquals(owner, repo.userName);
        assertEquals(repository, repo.repositoryName);
    }

    @Test
    public void gitAtUrlGitHub() {
	testURL("git@github.com:jenkinsci/jenkins.git", "github.com", "jenkinsci", "jenkins");
    }

    @Test
    public void gitAtUrlOtherHost() {
	testURL("git@gh.company.com:jenkinsci/jenkins.git", "gh.company.com", "jenkinsci", "jenkins");
    }

    @Test
    public void gitColonUrlGitHub() {
	testURL("git://github.com/jenkinsci/jenkins.git", "github.com", "jenkinsci", "jenkins");
    }

    @Test
    public void gitColonUrlOtherHost() {
	testURL("git://company.net/jenkinsci/jenkins.git", "company.net", "jenkinsci", "jenkins");
    }

    @Test
    public void httpsUrlGitHub() {
	testURL("https://user@github.com/jenkinsci/jenkins.git", "github.com", "jenkinsci", "jenkins");
    }
    
    @Test
    public void httpsUrlGitHubWithoutUser() {
	testURL("https://github.com/jenkinsci/jenkins.git", "github.com", "jenkinsci", "jenkins");
    }

    @Test
    public void httpsUrlOtherHost() {
	testURL("https://employee@gh.company.com/jenkinsci/jenkins.git", "gh.company.com", "jenkinsci", "jenkins");
    }

    @Test
    public void gitAtUrlGitHubNoSuffix() {
	testURL("git@github.com:jenkinsci/jenkins", "github.com", "jenkinsci", "jenkins");
    }

    @Test
    public void gitAtUrlOtherHostNoSuffix() {
	testURL("git@gh.company.com:jenkinsci/jenkins", "gh.company.com", "jenkinsci", "jenkins");
    }

    @Test
    public void gitColonUrlGitHubNoSuffix() {
	testURL("git://github.com/jenkinsci/jenkins", "github.com", "jenkinsci", "jenkins");
    }

    @Test
    public void gitColonUrlOtherHostNoSuffix() {
	testURL("git://company.net/jenkinsci/jenkins", "company.net", "jenkinsci", "jenkins");
    }

    @Test
    public void httpsUrlGitHubNoSuffix() {
	testURL("https://user@github.com/jenkinsci/jenkins", "github.com", "jenkinsci", "jenkins");
    }
    
    @Test
    public void httpsUrlGitHubWithoutUserNoSuffix() {
	testURL("https://github.com/jenkinsci/jenkins", "github.com", "jenkinsci", "jenkins");
    }

    @Test
    public void httpsUrlOtherHostNoSuffix() {
	testURL("https://employee@gh.company.com/jenkinsci/jenkins", "gh.company.com", "jenkinsci", "jenkins");
    }
    
    @Test
    public void gitAtUrlGitHubTrailingSlash() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("git@github.com:jenkinsci/jenkins/");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("github.com", repo.host);
    }

    @Test
    public void gitAtUrlOtherHostTrailingSlash() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("git@gh.company.com:jenkinsci/jenkins/");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("gh.company.com", repo.host);
    }

    @Test
    public void gitColonUrlGitHubTrailingSlash() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("git://github.com/jenkinsci/jenkins/");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("github.com", repo.host);
    }

    @Test
    public void gitColonUrlOtherHostTrailingSlash() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("git://company.net/jenkinsci/jenkins/");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("company.net", repo.host);
    }

    @Test
    public void httpsUrlGitHubTrailingSlash() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("https://user@github.com/jenkinsci/jenkins/");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("github.com", repo.host);
    }
    
    @Test
    public void httpsUrlGitHubWithoutUserTrailingSlash() {
        //this is valid for anonymous usage
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("https://github.com/jenkinsci/jenkins/");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("github.com", repo.host);
    }

    @Test
    public void httpsUrlOtherHostTrailingSlash() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("https://employee@gh.company.com/jenkinsci/jenkins/");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("gh.company.com", repo.host);
    }
    
    @Test
    public void trimWhitespace() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("               https://user@github.com/jenkinsci/jenkins/      ");
        assertNotNull(repo);
        assertEquals("jenkinsci", repo.userName);
        assertEquals("jenkins", repo.repositoryName);
        assertEquals("github.com", repo.host);
    }

    @Test
    public void badProtocol() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("gopher://gopher.floodgap.com");
        assertNull(repo);
    }

    @Test
    public void missingColon() {
        GitHubRepositoryName repo = GitHubRepositoryName
                .create("https//github.com/jenkinsci/jenkins");
        assertNull(repo);
    }

}

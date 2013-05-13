package com.coravy.hudson.plugins.github;


import com.cloudbees.jenkins.GitHubSCM;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: alon
 * Date: 5/12/13
 * Time: 5:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class GitHubSCMTest {

    @Test
    public void GitHubPollingCreateTest() {
        GitHubSCM repo = GitHubSCM.create("https://user:pass@github.com/kenshoo/github-plugin.git");
        assertNotNull(repo);
        assertEquals("user", repo.getUserName());
        assertEquals("pass", repo.getPassword());
        assertEquals("kenshoo/github-plugin", repo.getRepositoryName());
        assertEquals("https://api.github.com", repo.getUrl());
    }
}

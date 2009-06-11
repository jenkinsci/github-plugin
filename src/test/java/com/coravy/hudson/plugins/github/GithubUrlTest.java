package com.coravy.hudson.plugins.github;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GithubUrlTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public final void testBaseUrlWithTree() {
        GithubUrl url = new GithubUrl(
                "http://github.com/juretta/iphone-project-tools/tree/master");
        assertEquals("http://github.com/juretta/iphone-project-tools/", url
                .baseUrl());
        url = new GithubUrl(
                "http://github.com/juretta/iphone-project-tools/tree/unstable");
        assertEquals("http://github.com/juretta/iphone-project-tools/", url
                .baseUrl());
    }

    @Test
    public final void testBaseUrl() {
        GithubUrl url = new GithubUrl(
                "http://github.com/juretta/iphone-project-tools");
        assertEquals("http://github.com/juretta/iphone-project-tools/", url
                .baseUrl());
    }

    @Test
    public final void testCommitId() {
        GithubUrl url = new GithubUrl(
                "http://github.com/juretta/hudson-github-plugin/tree/master");
        assertEquals(
                "http://github.com/juretta/hudson-github-plugin/commit/5e31203faea681c41577b685818a361089fac1fc",
                url.commitId("5e31203faea681c41577b685818a361089fac1fc"));
    }
}

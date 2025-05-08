package com.coravy.hudson.plugins.github;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GithubUrlTest {

    @Test
    void testBaseUrlWithTree() {
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
    void testBaseUrl() {
        GithubUrl url = new GithubUrl(
                "http://github.com/juretta/iphone-project-tools");
        assertEquals("http://github.com/juretta/iphone-project-tools/", url
                .baseUrl());
    }

    @Test
    void testCommitId() {
        GithubUrl url = new GithubUrl(
                "http://github.com/juretta/hudson-github-plugin/tree/master");
        assertEquals(
                "http://github.com/juretta/hudson-github-plugin/commit/5e31203faea681c41577b685818a361089fac1fc",
                url.commitId("5e31203faea681c41577b685818a361089fac1fc"));
    }
}

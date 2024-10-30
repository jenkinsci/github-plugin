package com.cloudbees.jenkins;

import org.junit.Before;
import org.junit.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GitHubWebHookCrumbExclusionTest {

    private GitHubWebHookCrumbExclusion exclusion;
    private HttpServletRequest req;
    private HttpServletResponse resp;
    private FilterChain chain;

    @Before
    public void before() {
        exclusion = new GitHubWebHookCrumbExclusion();
        req = mock(HttpServletRequest.class);
        resp = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    @Test
    public void testFullPath() throws Exception {
        when(req.getPathInfo()).thenReturn("/github-webhook/");
        assertTrue(exclusion.process(req, resp, chain));
        verify(chain, times(1)).doFilter(req, resp);
    }

    @Test
    public void testFullPathWithoutSlash() throws Exception {
        when(req.getPathInfo()).thenReturn("/github-webhook");
        assertTrue(exclusion.process(req, resp, chain));
        verify(chain, times(1)).doFilter(req, resp);
    }

    @Test
    public void testInvalidPath() throws Exception {
        when(req.getPathInfo()).thenReturn("/some-other-url/");
        assertFalse(exclusion.process(req, resp, chain));
        verify(chain, never()).doFilter(req, resp);
    }

    @Test
    public void testNullPath() throws Exception {
        when(req.getPathInfo()).thenReturn(null);
        assertFalse(exclusion.process(req, resp, chain));
        verify(chain, never()).doFilter(req, resp);
    }

    @Test
    public void testEmptyPath() throws Exception {
        when(req.getPathInfo()).thenReturn("");
        assertFalse(exclusion.process(req, resp, chain));
        verify(chain, never()).doFilter(req, resp);
    }
}

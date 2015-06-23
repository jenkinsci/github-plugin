package com.cloudbees.jenkins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.jenkins.GitHubPushTrigger.DescriptorImpl;

/**
 * @author <a href = "mailto:achim.derigs@graudata.com">Achim Derigs</a>
 */
public final class GitHubPushTriggerTestCase {

    @Rule
    public final JenkinsRule j = new JenkinsRule();

    private static void triggerWebHook(final String repositoryUrl, final String pusherName)
            throws IOException {

        final DescriptorImpl descriptor = DescriptorImpl.get();
        final URL url = descriptor.getHookUrl();
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.connect();

        final String payload = "payload={\"repository\":{\"url\":\"" + repositoryUrl
                + "\"},\"pusher\":{\"name\":\"" + pusherName + "\",\"email\":\"\"}}";

        final OutputStream stream = connection.getOutputStream();

        try {
            stream.write(payload.getBytes(StandardCharsets.UTF_8));
        } finally {
            stream.close();
        }

        assertEquals(HttpURLConnection.HTTP_OK, connection.getResponseCode());
    }

    @Test
    public void trigger() throws IOException {
        final String repositoryUrl = "https://github.com/kohsuke/foo";
        final SCM scm = new GitSCM(repositoryUrl);
        final FreeStyleProject project = j.createFreeStyleProject();
        project.setScm(scm);

        final String expected = System.getProperty("user.name");
        final GitHubPushTrigger trigger = new GitHubPushTrigger();
        project.addTrigger(trigger);
        triggerWebHook(repositoryUrl, expected);

        final String actual = trigger.pushBy();
        assertEquals(expected, actual);
    }

    @Test
    public void ignore() throws IOException {
        final String repositoryUrl = "https://github.com/kohsuke/foo";
        final SCM scm = new GitSCM(repositoryUrl);
        final FreeStyleProject project = j.createFreeStyleProject();
        project.setScm(scm);

        final String pusherName = System.getProperty("user.name");
        final GitHubPushTrigger trigger = new GitHubPushTrigger(pusherName);
        project.addTrigger(trigger);
        triggerWebHook(repositoryUrl, pusherName);

        final Object object = trigger.pushBy();
        assertNull(object);
    }
}

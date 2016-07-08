package org.jenkinsci.plugins.github.webhook.subscriber;

import com.cloudbees.jenkins.GitHubPushTrigger;
import com.cloudbees.jenkins.GitHubRepositoryName;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import hudson.util.Secret;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;

import static org.jenkinsci.plugins.github.extension.CryptoUtil.computeSHA1Signature;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link DefaultPushGHEventSubscriberTest}
 * @author martinmine
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ JobInfoHelpers.class })
@PowerMockIgnore("javax.crypto.*")
public class DefaultPushGHEventSubscriberTest {
    private static final String PUSHER_NAME = "someone";
    private static final String PAYLOAD = "payload";

    private static final String REPO_URL = "git://host/user/repo.git";
    private static final GitSCM REPO_GIT_SCM = new GitSCM(REPO_URL);
    private static final GitHubRepositoryName REPO_NAME = GitHubRepositoryName.create(REPO_URL);

    private Secret globalSecret;
    private Secret projectSecret;

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Mock
    private GitHubPushTrigger trigger;

    @Before
    public void setup() throws IOException {
        globalSecret = Secret.fromString("globalSecret");
        projectSecret = Secret.fromString("projectSecret");

        final FreeStyleProject job = jenkinsRule.createFreeStyleProject();
        job.setScm(REPO_GIT_SCM);

        PowerMockito.mockStatic(JobInfoHelpers.class);
        PowerMockito.when(JobInfoHelpers.triggerFrom(job, GitHubPushTrigger.class)).thenReturn(trigger);
    }

    @Test
    public void jobsWithoutSecretsAreExecuted() throws Exception {
        new DefaultPushGHEventSubscriber()
                .triggerJobs(REPO_NAME, null, PUSHER_NAME, null);

        verify(trigger, times(1)).onPost(PUSHER_NAME);
    }

    @Test
    public void shouldNotRunJobsWithoutRequiredProjectSignature() throws Exception {
        when(trigger.getSharedSecret()).thenReturn(projectSecret);

        new DefaultPushGHEventSubscriber()
                .triggerJobs(REPO_NAME, null, PUSHER_NAME, null);

        verify(trigger, never()).onPost(anyString());
    }

    @Test
    public void shouldNotRunJobsWithoutRequiredGlobalSignature() throws Exception {
        GitHubPlugin.configuration().setGloballySharedSecret(globalSecret);

        new DefaultPushGHEventSubscriber()
                .triggerJobs(REPO_NAME, null, PUSHER_NAME, null);

        verify(trigger, never()).onPost(anyString());
    }

    @Test
    public void shouldRunJobsWithValidProjectSignature() throws Exception {
        when(trigger.getSharedSecret()).thenReturn(projectSecret);

        new DefaultPushGHEventSubscriber()
                .triggerJobs(REPO_NAME, PAYLOAD, PUSHER_NAME, createSignature(PAYLOAD, projectSecret));

        verify(trigger, times(1)).onPost(PUSHER_NAME);
    }

    @Test
    public void shouldRunJobsWithValidGlobalSignature() throws Exception {
        GitHubPlugin.configuration().setGloballySharedSecret(globalSecret);

        new DefaultPushGHEventSubscriber()
                .triggerJobs(REPO_NAME, PAYLOAD, PUSHER_NAME, createSignature(PAYLOAD, globalSecret));

        verify(trigger, times(1)).onPost(PUSHER_NAME);
    }

    @Test
    public void shouldOverrideGlobalSecretWhenProjectSecretIsSpecified() throws Exception {
        GitHubPlugin.configuration().setGloballySharedSecret(globalSecret);
        when(trigger.getSharedSecret()).thenReturn(projectSecret);

        new DefaultPushGHEventSubscriber()
                .triggerJobs(REPO_NAME, PAYLOAD, PUSHER_NAME, createSignature(PAYLOAD, projectSecret));

        verify(trigger, times(1)).onPost(PUSHER_NAME);
    }

    @Test
    public void shouldNotRunJobsWithInvalidRequiredSignature() throws Exception {
        GitHubPlugin.configuration().setGloballySharedSecret(globalSecret);
        when(trigger.getSharedSecret()).thenReturn(projectSecret);

        new DefaultPushGHEventSubscriber()
                .triggerJobs(REPO_NAME, PAYLOAD, PUSHER_NAME, "sha1=a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");

        verify(trigger, never()).onPost(anyString());
    }

    private String createSignature(final String payload, final Secret secret) {
        return "sha1=" + computeSHA1Signature(payload, secret);
    }
}
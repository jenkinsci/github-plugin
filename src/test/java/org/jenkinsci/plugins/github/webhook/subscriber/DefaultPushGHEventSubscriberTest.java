package org.jenkinsci.plugins.github.webhook.subscriber;

import com.cloudbees.jenkins.GitHubPushTrigger;
import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;
import hudson.model.ItemGroup;
import hudson.model.Job;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.jenkinsci.plugins.github.extension.CryptoUtil.computeSHA1Signature;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link DefaultPushGHEventSubscriberTest}
 * @author martinmine
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Jenkins.class, GitHubRepositoryNameContributor.class, JobInfoHelpers.class, GitHubPlugin.class })
@PowerMockIgnore("javax.crypto.*")
public class DefaultPushGHEventSubscriberTest {
    private static final String PUSHER_NAME = "someone";
    private static final String GLOBAL_SECRET = "globalSecret";
    private static final String PROJECT_SECRET = "projectSecret";
    private static final String PAYLOAD = "payload";

    @Mock
    private Jenkins jenkins;

    @Mock
    private Job<?, ?> job;

    @Mock
    private GitHubPushTrigger trigger;

    @Mock
    private GitHubRepositoryName requestingRepoName;

    @Mock
    private Collection<GitHubRepositoryName> registeredRepositories;

    @Mock
    private GitHubPluginConfig config;

    @Before
    public void setup() {
        PowerMockito.mockStatic(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);

        PowerMockito.mockStatic(GitHubPlugin.class);
        PowerMockito.when(GitHubPlugin.configuration()).thenReturn(config);

        PowerMockito.mockStatic(JobInfoHelpers.class);
        PowerMockito.when(JobInfoHelpers.triggerFrom(job, GitHubPushTrigger.class)).thenReturn(trigger);

        final List<Job> jobs = new LinkedList<>();
        jobs.add(job);

        when(jenkins.getAllItems(Job.class)).thenReturn(jobs);

        final ItemGroup itemGroup = mock(ItemGroup.class);
        when(itemGroup.getFullDisplayName()).thenReturn("sample");

        when(job.getParent()).thenReturn(itemGroup);

        PowerMockito.mockStatic(GitHubRepositoryNameContributor.class);
        PowerMockito.when(GitHubRepositoryNameContributor.parseAssociatedNames(job)).thenReturn(registeredRepositories);
    }

    @Test
    public void jobsWithoutSecretsAreExecuted() throws Exception {
        when(registeredRepositories.contains(requestingRepoName)).thenReturn(true);

        new DefaultPushGHEventSubscriber()
                .triggerJobs(requestingRepoName, null, PUSHER_NAME, null);

        verify(trigger, times(1)).onPost(PUSHER_NAME);
    }

    @Test
    public void shouldNotRunJobsWithoutRequiredProjectSignature() throws Exception {
        when(registeredRepositories.contains(requestingRepoName)).thenReturn(true);
        when(trigger.getSharedSecret()).thenReturn(PROJECT_SECRET);

        new DefaultPushGHEventSubscriber()
                .triggerJobs(requestingRepoName, null, PUSHER_NAME, null);

        verify(trigger, times(0)).onPost(anyString());
    }

    @Test
    public void shouldNotRunJobsWithoutRequiredGlobalSignature() throws Exception {
        when(registeredRepositories.contains(requestingRepoName)).thenReturn(true);
        when(config.getGloballySharedSecret()).thenReturn(GLOBAL_SECRET);

        new DefaultPushGHEventSubscriber()
                .triggerJobs(requestingRepoName, null, PUSHER_NAME, null);

        verify(trigger, times(0)).onPost(anyString());
    }

    @Test
    public void shouldRunJobsWithValidProjectSignature() throws Exception {
        when(registeredRepositories.contains(requestingRepoName)).thenReturn(true);
        when(trigger.getSharedSecret()).thenReturn(PROJECT_SECRET);

        new DefaultPushGHEventSubscriber()
                .triggerJobs(requestingRepoName, PAYLOAD, PUSHER_NAME, createSignature(PAYLOAD, PROJECT_SECRET));

        verify(trigger, times(1)).onPost(PUSHER_NAME);
    }

    @Test
    public void shouldRunJobsWithValidGlobalSignature() throws Exception {
        when(registeredRepositories.contains(requestingRepoName)).thenReturn(true);
        when(config.getGloballySharedSecret()).thenReturn(GLOBAL_SECRET);

        new DefaultPushGHEventSubscriber()
                .triggerJobs(requestingRepoName, PAYLOAD, PUSHER_NAME, createSignature(PAYLOAD, GLOBAL_SECRET));

        verify(trigger, times(1)).onPost(PUSHER_NAME);
    }

    @Test
    public void shouldOverrideGlobalSecretWhenProjectSecretIsSpecified() throws Exception {
        when(registeredRepositories.contains(requestingRepoName)).thenReturn(true);
        when(config.getGloballySharedSecret()).thenReturn(GLOBAL_SECRET);
        when(trigger.getSharedSecret()).thenReturn(PROJECT_SECRET);

        new DefaultPushGHEventSubscriber()
                .triggerJobs(requestingRepoName, PAYLOAD, PUSHER_NAME, createSignature(PAYLOAD, PROJECT_SECRET));

        verify(trigger, times(1)).onPost(PUSHER_NAME);
    }

    @Test
    public void shouldNotRunJobsWithInvalidRequiredSignature() throws Exception {
        when(registeredRepositories.contains(requestingRepoName)).thenReturn(true);
        when(config.getGloballySharedSecret()).thenReturn(GLOBAL_SECRET);
        when(trigger.getSharedSecret()).thenReturn(PROJECT_SECRET);

        new DefaultPushGHEventSubscriber()
                .triggerJobs(requestingRepoName, PAYLOAD, PUSHER_NAME, "sha1=a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");

        verify(trigger, times(0)).onPost(anyString());
    }

    private String createSignature(final String payload, final String secret) {
        return "sha1=" + computeSHA1Signature(payload, secret);
    }
}
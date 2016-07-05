package org.jenkinsci.plugins.github.webhook.subscriber;

import com.cloudbees.jenkins.GitHubPushTrigger;
import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;
import hudson.DescriptorExtensionList;
import hudson.model.ItemGroup;
import hudson.model.Job;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.util.JobInfoHelpers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
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
public class DefaultPushGHEventSubscriberTest {

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

    private static final String PUSHER_NAME = "someone";
    private static final String GLOBAL_SECRET = "globalSecret";
    private static final String PROJECT_SECRET = "projectSecret";
    private static final String PAYLOAD = "payload";

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
        when(jenkins.<GlobalConfiguration, GlobalConfiguration>getDescriptorList(GlobalConfiguration.class)).thenReturn(mock(DescriptorExtensionList.class));

        final ItemGroup g = mock(ItemGroup.class);
        when(g.getFullDisplayName()).thenReturn("sample");

        when(job.getParent()).thenReturn(g);

        PowerMockito.mockStatic(GitHubRepositoryNameContributor.class);
        PowerMockito.when(GitHubRepositoryNameContributor.parseAssociatedNames(job)).thenReturn(registeredRepositories);
    }

    @Test
    public void jobsWithoutSecretsAreExecuted() throws Exception {
        final DefaultPushGHEventSubscriber subscriber = new DefaultPushGHEventSubscriber();

        when(registeredRepositories.contains(requestingRepoName)).thenReturn(true);

        subscriber.triggerJobs(requestingRepoName, null, PUSHER_NAME, null);

        verify(trigger, times(1)).onPost(PUSHER_NAME);
    }

    @Test
    public void shouldNotRunJobsWithoutRequiredProjectSignature() throws Exception {
        final DefaultPushGHEventSubscriber subscriber = new DefaultPushGHEventSubscriber();

        when(trigger.getSharedSecret()).thenReturn(PROJECT_SECRET);

        subscriber.triggerJobs(requestingRepoName, null, PUSHER_NAME, null);

        verify(trigger, times(0)).onPost(anyString());
    }

    @Test
    public void shouldNotRunJobsWithoutRequiredGlobalSignature() throws Exception {
        final DefaultPushGHEventSubscriber subscriber = new DefaultPushGHEventSubscriber();

        when(config.getGloballySharedSecret()).thenReturn(GLOBAL_SECRET);

        subscriber.triggerJobs(requestingRepoName, null, PUSHER_NAME, null);

        verify(trigger, times(0)).onPost(anyString());
    }

    @Test
    public void shouldRunJobsWithValidProjectSignature() throws Exception {
        final DefaultPushGHEventSubscriber subscriber = new DefaultPushGHEventSubscriber();

        when(trigger.getSharedSecret()).thenReturn(PROJECT_SECRET);

        subscriber.triggerJobs(requestingRepoName, PAYLOAD, PUSHER_NAME, computeSHA1Signature(PAYLOAD, PROJECT_SECRET));

        verify(trigger, times(1)).onPost(PUSHER_NAME);
    }

    @Test
    public void shouldRunJobsWithValidGlobalSignature() throws Exception {
        final DefaultPushGHEventSubscriber subscriber = new DefaultPushGHEventSubscriber();

        when(config.getGloballySharedSecret()).thenReturn(GLOBAL_SECRET);

        subscriber.triggerJobs(requestingRepoName, PAYLOAD, PUSHER_NAME, computeSHA1Signature(PAYLOAD, GLOBAL_SECRET));

        verify(trigger, times(1)).onPost(PUSHER_NAME);
    }

    @Test
    public void shouldOverrideGlobalSecretWhenProjectSecretIsSpecified() throws Exception {
        final DefaultPushGHEventSubscriber subscriber = new DefaultPushGHEventSubscriber();

        when(config.getGloballySharedSecret()).thenReturn(GLOBAL_SECRET);
        when(trigger.getSharedSecret()).thenReturn(PROJECT_SECRET);

        subscriber.triggerJobs(requestingRepoName, PAYLOAD, PUSHER_NAME, computeSHA1Signature(PAYLOAD, PROJECT_SECRET));

        verify(trigger, times(1)).onPost(PUSHER_NAME);
    }

    @Test
    public void shouldNotRunJobsWithInvalidRequiredSignature() throws Exception {
        final DefaultPushGHEventSubscriber subscriber = new DefaultPushGHEventSubscriber();

        when(config.getGloballySharedSecret()).thenReturn(GLOBAL_SECRET);
        when(trigger.getSharedSecret()).thenReturn(PROJECT_SECRET);

        subscriber.triggerJobs(requestingRepoName, PAYLOAD, PUSHER_NAME, computeSHA1Signature(PAYLOAD, PROJECT_SECRET));

        verify(trigger, times(0)).onPost(anyString());
    }
}
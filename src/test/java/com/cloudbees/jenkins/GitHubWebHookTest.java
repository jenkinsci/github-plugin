package com.cloudbees.jenkins;

import com.google.inject.Inject;

import hudson.model.AbstractProject;
import hudson.model.Job;

import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.github.GHEvent;

import java.util.Set;

import static com.google.common.collect.Sets.immutableEnumSet;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubWebHookTest {

    public static final String PAYLOAD = "{}";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Inject
    private IssueSubscriber subscriber;

    @Inject
    private PullRequestSubscriber pullRequestSubscriber;

    @Inject
    private ThrowablePullRequestSubscriber throwablePullRequestSubscriber;

    @Before
    public void setUp() throws Exception {
        jenkins.getInstance().getInjector().injectMembers(this);
    }

    @Test
    public void shouldCallExtensionInterestedInIssues() throws Exception {
        new GitHubWebHook().doIndex(GHEvent.ISSUES, PAYLOAD, null);
        assertThat("should get interested event", subscriber.lastEvent(), equalTo(GHEvent.ISSUES));
    }

    @Test
    public void shouldNotCallAnyExtensionsWithPublicEventIfNotRegistered() throws Exception {
        new GitHubWebHook().doIndex(GHEvent.PUBLIC, PAYLOAD, null);
        assertThat("should not get not interested event", subscriber.lastEvent(), nullValue());
    }

    @Test
    public void shouldCatchThrowableOnFailedSubscriber() throws Exception {
        new GitHubWebHook().doIndex(GHEvent.PULL_REQUEST, PAYLOAD, null);
        assertThat("each extension should get event",
                asList(
                        pullRequestSubscriber.lastEvent(),
                        throwablePullRequestSubscriber.lastEvent()
                ), everyItem(equalTo(GHEvent.PULL_REQUEST)));
    }

    @TestExtension
    @SuppressWarnings("unused")
    public static class IssueSubscriber extends TestSubscriber {

        public IssueSubscriber() {
            super(GHEvent.ISSUES);
        }
    }

    @TestExtension
    @SuppressWarnings("unused")
    public static class PullRequestSubscriber extends TestSubscriber {

        public PullRequestSubscriber() {
            super(GHEvent.PULL_REQUEST);
        }
    }

    @TestExtension
    @SuppressWarnings("unused")
    public static class ThrowablePullRequestSubscriber extends TestSubscriber {

        public ThrowablePullRequestSubscriber() {
            super(GHEvent.PULL_REQUEST);
        }

        @Override
        protected void onEvent(GHEvent event, String payload, String signature) {
            super.onEvent(event, payload, signature);
            throw new GotEventException("Something went wrong!");
        }
    }

    public static class TestSubscriber extends GHEventsSubscriber {

        private GHEvent interested;
        private GHEvent event;

        public TestSubscriber(GHEvent interested) {
            this.interested = interested;
        }

        @Override
        protected boolean isApplicable(Job<?, ?> project) {
            return true;
        }

        @Override
        protected Set<GHEvent> events() {
            return immutableEnumSet(interested);
        }

        @Override
        protected void onEvent(GHEvent event, String payload, String signature) {
            this.event = event;
        }

        public GHEvent lastEvent() {
            return event;
        }
    }

    public static class GotEventException extends RuntimeException {
        public GotEventException(String message) {
            super(message);
        }
    }
}

package com.cloudbees.jenkins;

import com.google.inject.Inject;

import hudson.model.Item;

import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.github.GHEvent;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static com.google.common.collect.Sets.immutableEnumSet;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

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

    @Mock
    private StaplerRequest2 req2;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        jenkins.getInstance().getInjector().injectMembers(this);
    }

    @Test
    public void shouldCallExtensionInterestedInIssues() throws Exception {
        try(var mockedStapler = Mockito.mockStatic(Stapler.class)) {
            mockedStapler.when(Stapler::getCurrentRequest2).thenReturn(req2);

            new GitHubWebHook().doIndex(GHEvent.ISSUES, PAYLOAD);
            assertThat("should get interested event", subscriber.lastEvent(), equalTo(GHEvent.ISSUES));
        }
    }

    @Test
    public void shouldNotCallAnyExtensionsWithPublicEventIfNotRegistered() throws Exception {
        try(var mockedStapler = Mockito.mockStatic(Stapler.class)) {
            mockedStapler.when(Stapler::getCurrentRequest2).thenReturn(req2);

            new GitHubWebHook().doIndex(GHEvent.PUBLIC, PAYLOAD);
            assertThat("should not get not interested event", subscriber.lastEvent(), nullValue());
        }
    }

    @Test
    public void shouldCatchThrowableOnFailedSubscriber() throws Exception {
        try(var mockedStapler = Mockito.mockStatic(Stapler.class)) {
            mockedStapler.when(Stapler::getCurrentRequest2).thenReturn(req2);

            new GitHubWebHook().doIndex(GHEvent.PULL_REQUEST, PAYLOAD);
            assertThat("each extension should get event",
                       asList(pullRequestSubscriber.lastEvent(), throwablePullRequestSubscriber.lastEvent()),
                       everyItem(equalTo(GHEvent.PULL_REQUEST)));
        }
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
        protected void onEvent(GHEvent event, String payload) {
            super.onEvent(event, payload);
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
        protected boolean isApplicable(Item project) {
            return true;
        }

        @Override
        protected Set<GHEvent> events() {
            return immutableEnumSet(interested);
        }

        @Override
        protected void onEvent(GHEvent event, String payload) {
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

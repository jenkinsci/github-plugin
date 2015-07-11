package com.cloudbees.jenkins;

import hudson.model.AbstractProject;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.github.GHEvent;

import java.util.Set;

import static com.google.common.collect.Sets.immutableEnumSet;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubWebHookTest {

    public static final String PAYLOAD = "{}";
    
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test(expected = GotEventException.class)
    public void shouldCallExtensionInterestedInIssues() throws Exception {
        new GitHubWebHook().doIndex(GHEvent.ISSUES, PAYLOAD);
    }

    @Test
    public void shouldNotCallAnyExtensionsWithPublicEventIfNotRegistered() throws Exception {
        new GitHubWebHook().doIndex(GHEvent.PUBLIC, PAYLOAD);
    }

    @TestExtension
    @SuppressWarnings("unused")
    public static class IssueSubscriber extends GHEventsSubscriber {
        @Override
        protected boolean isApplicable(AbstractProject<?, ?> project) {
            return true;
        }

        @Override
        protected Set<GHEvent> events() {
            return immutableEnumSet(GHEvent.ISSUES);
        }

        @Override
        protected void onEvent(GHEvent event, String payload) {
            throw new GotEventException(String.format("got event %s", event));
        }
    }

    public static class GotEventException extends RuntimeException {
        public GotEventException(String message) {
            super(message);
        }
    }
}

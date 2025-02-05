package org.jenkinsci.plugins.github.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.net.URL;

import org.htmlunit.HttpMethod;

import org.htmlunit.WebRequest;
import org.jenkinsci.plugins.github.Messages;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.jenkinsci.plugins.github.webhook.subscriber.DuplicateEventsSubscriber;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.mockito.Mockito;

public class GitHubDuplicateEventsMonitorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    private WebClient wc;

    @Before
    public void setUp() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        wc = j.createWebClient();
        wc.login("admin", "admin");
    }

    @Test
    public void testAdminMonitorDisplaysForDuplicateEvents() throws Exception {
        try (var mockSubscriber = Mockito.mockStatic(GHEventsSubscriber.class)) {
            var subscribers = j.jenkins.getExtensionList(GHEventsSubscriber.class);
            var nonDuplicateSubscribers = subscribers.stream()
                                                     .filter(e -> !(e instanceof DuplicateEventsSubscriber))
                                                     .toList();
            nonDuplicateSubscribers.forEach(subscribers::remove);
            mockSubscriber.when(GHEventsSubscriber::all).thenReturn(subscribers);

            // to begin with, monitor doesn't show automatically
            assertMonitorNotDisplayed();

            // normal case: unique events don't cause admin monitor
            sendGHEvents(wc, "event1");
            sendGHEvents(wc, "event2");
            assertMonitorNotDisplayed();

            // duplicate events cause admin monitor
            sendGHEvents(wc, "event3");
            sendGHEvents(wc, "event3");
            assertMonitorDisplayed();
        }
    }

    private void sendGHEvents(WebClient wc, String eventGuid) throws IOException {
        wc.addRequestHeader("Content-Type", "application/json");
        wc.addRequestHeader("X-GitHub-Delivery", eventGuid);
        wc.addRequestHeader("X-Github-Event", "push");
        String url = j.getURL() + "/github-webhook/";
        var webRequest = new WebRequest(new URL(url), HttpMethod.POST);
        webRequest.setRequestBody("{}");
        wc.getPage(webRequest).getWebResponse();
    }

    private void assertMonitorNotDisplayed() throws IOException {
        String manageUrl = j.getURL() + "/manage";
        assertThat(
            wc.getPage(manageUrl).getWebResponse().getContentAsString(),
            not(containsString(Messages.duplicate_events_administrative_monitor_blurb())));
    }

    private void assertMonitorDisplayed() throws IOException {
        String manageUrl = j.getURL() + "/manage";
        assertThat(
            wc.getPage(manageUrl).getWebResponse().getContentAsString(),
            containsString(Messages.duplicate_events_administrative_monitor_blurb()));
    }
}

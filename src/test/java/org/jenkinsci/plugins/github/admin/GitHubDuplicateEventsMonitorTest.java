package org.jenkinsci.plugins.github.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URL;

import org.htmlunit.HttpMethod;

import org.htmlunit.WebRequest;
import org.htmlunit.html.HtmlElementUtil;
import org.htmlunit.html.HtmlPage;
import org.jenkinsci.plugins.github.Messages;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import hudson.ExtensionList;

@WithJenkins
class GitHubDuplicateEventsMonitorTest {

    private JenkinsRule j;

    private GitHubDuplicateEventsMonitor monitor;
    private WebClient wc;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        monitor = ExtensionList.lookupSingleton(GitHubDuplicateEventsMonitor.class);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        wc = j.createWebClient();
        wc.login("admin", "admin");
    }

    @Test
    void testAdminMonitorDisplaysForDuplicateEvents() throws Exception {
        try (var mockSubscriber = Mockito.mockStatic(GHEventsSubscriber.class)) {
            var subscribers = j.jenkins.getExtensionList(GHEventsSubscriber.class);
            /* Other type of subscribers are removed to avoid them invoking event processing. At this
            time, when using the `push` event type, the `DefaultGHEventsSubscriber` gets invoked, and throws
            an NPE during processing of the event. This is because the `GHEvent` object here is not fully initialized.
            However, as this test is only concerned with the duplicate event detection, it doesn't seem to add value
            in fixing for the NPE. Alternatively, we may choose to send an event which is not subscribed
            by other subscribers (ex: `check_run`), but that would only work until someone adds a new subscriber for
            that event type, at which point, a new event type would need to be chosen in here.
            * */
            var nonDuplicateSubscribers = subscribers.stream()
                                                     .filter(e -> !(e instanceof GitHubDuplicateEventsMonitor.DuplicateEventsSubscriber))
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
            var event3 = "event3";
            sendGHEvents(wc, event3);
            sendGHEvents(wc, event3);
            assertMonitorDisplayed(event3);

            // send a new duplicate
            var event4 = "event4";
            sendGHEvents(wc, event4);
            sendGHEvents(wc, event4);
            assertMonitorDisplayed(event4);
        }
    }

    private void sendGHEvents(WebClient wc, String eventGuid) throws IOException {
        wc.addRequestHeader("Content-Type", "application/json");
        wc.addRequestHeader("X-GitHub-Delivery", eventGuid);
        wc.addRequestHeader("X-Github-Event", "push");
        String url = j.getURL() + "/github-webhook/";
        var webRequest = new WebRequest(new URL(url), HttpMethod.POST);
        webRequest.setRequestBody(getJsonPayload(eventGuid));
        assertThat(wc.getPage(webRequest).getWebResponse().getStatusCode(), is(200));
    }

    private void assertMonitorNotDisplayed() throws IOException, SAXException {
        String manageUrl = j.getURL() + "/manage";
        assertThat(
            wc.getPage(manageUrl).getWebResponse().getContentAsString(),
            not(containsString(Messages.duplicate_events_administrative_monitor_blurb(
                GitHubDuplicateEventsMonitor.LAST_DUPLICATE_CLICK_HERE_ANCHOR_ID,
                monitor.getLastDuplicateUrl()
            ))));
        assertEquals(GitHubDuplicateEventsMonitor.getLastDuplicateNoEventPayload().toString(),
                     getLastDuplicatePageContentByLink());
    }

    private void assertMonitorDisplayed(String eventGuid) throws IOException, SAXException {
        String manageUrl = j.getURL() + "/manage";
        assertThat(
            wc.getPage(manageUrl).getWebResponse().getContentAsString(),
            containsString(Messages.duplicate_events_administrative_monitor_blurb(
                GitHubDuplicateEventsMonitor.LAST_DUPLICATE_CLICK_HERE_ANCHOR_ID,
                monitor.getLastDuplicateUrl())));
        assertEquals(getJsonPayload(eventGuid), getLastDuplicatePageContentByAnchor());
    }

    private String getLastDuplicatePageContentByAnchor() throws IOException, SAXException {
        HtmlPage page = wc.goTo("./manage");
        var lastDuplicateAnchor = page.getAnchors().stream().filter(
            a -> a.getId().equals(GitHubDuplicateEventsMonitor.LAST_DUPLICATE_CLICK_HERE_ANCHOR_ID)
            ).findFirst();
        var lastDuplicatePage = HtmlElementUtil.click(lastDuplicateAnchor.get());
        return lastDuplicatePage.getWebResponse().getContentAsString();
    }

    private String getLastDuplicatePageContentByLink() throws IOException, SAXException {
        return wc.goTo(monitor.getLastDuplicateUrl(), "application/json").getWebResponse().getContentAsString();
    }

    private String getJsonPayload(String eventGuid) {
            return "{\"payload\":\"" + eventGuid + "\"}";
    }
}

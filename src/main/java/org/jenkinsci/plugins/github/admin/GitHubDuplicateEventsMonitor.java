package org.jenkinsci.plugins.github.admin;

import com.google.common.annotations.VisibleForTesting;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.Messages;
import org.jenkinsci.plugins.github.webhook.subscriber.DuplicateEventsSubscriber;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.json.JsonHttpResponse;
import org.kohsuke.stapler.verb.GET;
import net.sf.json.JSONObject;

@SuppressWarnings("unused")
@Extension
public class GitHubDuplicateEventsMonitor extends AdministrativeMonitor {

    @VisibleForTesting
    static final String LAST_DUPLICATE_CLICK_HERE_ANCHOR_ID = GitHubDuplicateEventsMonitor.class.getName()
                                                              + "#last-duplicate";

    @Override
    public String getDisplayName() {
        return Messages.duplicate_events_administrative_monitor_displayname();
    }

    public String getDescription() {
        return Messages.duplicate_events_administrative_monitor_description();
    }

    public String getBlurb() {
        return Messages.duplicate_events_administrative_monitor_blurb(
            LAST_DUPLICATE_CLICK_HERE_ANCHOR_ID, this.getLastDuplicateUrl());
    }

    @VisibleForTesting
    String getLastDuplicateUrl() {
        return this.getUrl() + "/" + "last-duplicate.json";
    }

    @Override
    public boolean isActivated() {
        return DuplicateEventsSubscriber.isDuplicateEventSeen();
    }

    @Override
    public boolean hasRequiredPermission() {
        return Jenkins.get().hasPermission(Jenkins.SYSTEM_READ);
    }

    @Override
    public void checkRequiredPermission() {
        Jenkins.get().checkPermission(Jenkins.SYSTEM_READ);
    }

    @GET
    @WebMethod(name = "last-duplicate.json")
    public HttpResponse doGetLastDuplicatePayload() {
        Jenkins.get().checkPermission(Jenkins.SYSTEM_READ);
        JSONObject data = getLastDuplicateNoEventPayload();

        var lastDuplicate = DuplicateEventsSubscriber.getLastDuplicate();
        if (lastDuplicate != null) {
            data = JSONObject.fromObject(lastDuplicate.ghSubscriberEvent().getPayload());
        }
        return new JsonHttpResponse(data, 200);
    }

    @VisibleForTesting
    static JSONObject getLastDuplicateNoEventPayload() {
        return JSONObject.fromObject("{\"payload\": \"No duplicate events seen yet.\"}");
    }
}

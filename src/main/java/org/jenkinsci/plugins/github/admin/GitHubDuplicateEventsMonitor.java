package org.jenkinsci.plugins.github.admin;

import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.Messages;
import org.jenkinsci.plugins.github.webhook.subscriber.DuplicateEventsSubscriber;

@SuppressWarnings("unused")
@Extension
public class GitHubDuplicateEventsMonitor extends AdministrativeMonitor {

    private static final Logger LOGGER = Logger.getLogger(GitHubDuplicateEventsMonitor.class.getName());
    private static String previouslyLoggedEventId;

    @Override
    public String getDisplayName() {
        return Messages.duplicate_events_administrative_monitor_displayname();
    }

    public String getDescription() {
        return Messages.duplicate_events_administrative_monitor_description();
    }

    public String getBlurb() {
        return Messages.duplicate_events_administrative_monitor_blurb();
    }

    @Override
    public boolean isActivated() {
        boolean isActivated = DuplicateEventsSubscriber.isDuplicateEventSeen();
        /* The `isActivated` method is evaluated by Jenkins a lot of times when the user is navigating various pages.
            So when the `FINEST` logger is enabled, we should avoid logging the same event multiple times.
         */
        if (isActivated) {
            var curDuplicate = DuplicateEventsSubscriber.getLastDuplicate();
            if (!curDuplicate.eventGuid().equals(previouslyLoggedEventId)) {
                LOGGER.finest(() -> {
                    previouslyLoggedEventId = curDuplicate.eventGuid();
                    return "Latest tracked duplicate event id: " + curDuplicate.eventGuid()
                           + ", payload: " + curDuplicate.ghSubscriberEvent().getPayload();
                });
            }
        }
        return isActivated;
    }

    @Override
    public boolean hasRequiredPermission() {
        return Jenkins.get().hasPermission(Jenkins.SYSTEM_READ);
    }

    @Override
    public void checkRequiredPermission() {
        Jenkins.get().checkPermission(Jenkins.SYSTEM_READ);
    }
}

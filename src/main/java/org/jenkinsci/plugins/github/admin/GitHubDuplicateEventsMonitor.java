package org.jenkinsci.plugins.github.admin;

import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.Messages;
import org.jenkinsci.plugins.github.webhook.subscriber.DuplicateEventsSubscriber;

@SuppressWarnings("unused")
@Extension
public class GitHubDuplicateEventsMonitor extends AdministrativeMonitor {

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
        return !DuplicateEventsSubscriber.getDuplicateEventCounts().isEmpty();
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

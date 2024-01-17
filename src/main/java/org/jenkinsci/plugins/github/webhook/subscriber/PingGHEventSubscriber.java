package org.jenkinsci.plugins.github.webhook.subscriber;

import com.cloudbees.jenkins.GitHubRepositoryName;
import hudson.Extension;
import hudson.model.Item;
import java.io.IOException;
import java.io.StringReader;
import java.util.Set;
import jakarta.inject.Inject;
import org.jenkinsci.plugins.github.admin.GitHubHookRegisterProblemMonitor;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Sets.immutableEnumSet;
import static org.kohsuke.github.GHEvent.PING;

/**
 * Get ping events to log them
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.13.0
 */
@Extension
@SuppressWarnings("unused")
public class PingGHEventSubscriber extends GHEventsSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(PingGHEventSubscriber.class);

    @Inject
    private transient GitHubHookRegisterProblemMonitor monitor;

    /**
     * This subscriber is not applicable to any item
     *
     * @param project ignored
     * @return always false
     */
    @Override
    protected boolean isApplicable(Item project) {
        return false;
    }

    /**
     * @return set with only ping event
     */
    @Override
    protected Set<GHEvent> events() {
        return immutableEnumSet(PING);
    }

    /**
     * Logs repo on ping event
     *
     * @param event   only PING event
     * @param payload payload of gh-event. Never blank
     */
    @Override
    protected void onEvent(GHEvent event, String payload) {
        GHEventPayload.Ping ping;
        try {
            ping = GitHub.offline().parseEventPayload(new StringReader(payload), GHEventPayload.Ping.class);
        } catch (IOException e) {
            LOGGER.warn("Received malformed PingEvent: " + payload, e);
            return;
        }
        GHRepository repository = ping.getRepository();
        if (repository != null) {
            LOGGER.info("{} webhook received from repo <{}>!", event, repository.getHtmlUrl());
            monitor.resolveProblem(GitHubRepositoryName.create(repository.getHtmlUrl().toExternalForm()));
        } else {
            GHOrganization organization = ping.getOrganization();
            if (organization != null) {
                LOGGER.info("{} webhook received from org <{}>!", event, organization.getUrl());
            } else {
                LOGGER.warn("{} webhook received with unexpected payload", event);
            }
        }
    }
}

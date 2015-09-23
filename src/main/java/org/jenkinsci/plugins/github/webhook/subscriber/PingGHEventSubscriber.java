package org.jenkinsci.plugins.github.webhook.subscriber;

import hudson.Extension;
import hudson.model.Job;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.kohsuke.github.GHEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.collect.Sets.immutableEnumSet;
import net.sf.json.JSONObject;
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

    /**
     * This subscriber is not applicable to any job
     *
     * @param project ignored
     *
     * @return always false
     */
    @Override
    protected boolean isApplicable(Job<?, ?> project) {
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
        JSONObject parsedPayload = JSONObject.fromObject(payload);
        JSONObject repository = parsedPayload.optJSONObject("repository");
        if (repository != null) {
            // something like <https://github.com/bar/foo>
            LOGGER.info("{} webhook received from repo <{}>!", event, repository.getString("url"));
        } else {
            JSONObject organization = parsedPayload.optJSONObject("organization");
            if (organization != null) {
                LOGGER.info("{} webhook received from org <{}>!", event, organization.getString("url"));
            } else {
                LOGGER.warn("{} webhook received with unexpected payload", event);
            }
        }
    }
}

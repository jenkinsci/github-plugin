package org.jenkinsci.plugins.github.webhook.subscriber;

import hudson.Extension;
import hudson.model.AbstractProject;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.kohsuke.github.GHEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.collect.Sets.immutableEnumSet;
import static net.sf.json.JSONObject.fromObject;
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
    protected boolean isApplicable(AbstractProject<?, ?> project) {
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
        // something like <https://github.com/bar/foo>
        String repo = fromObject(payload).getJSONObject("repository").getString("url");
        LOGGER.info("{} webhook received from repo <{}>!", event, repo);
    }
}

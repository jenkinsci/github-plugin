package org.jenkinsci.plugins.github.webhook.subscriber;

import com.cloudbees.jenkins.GitHubRepositoryName;
import hudson.Extension;
import hudson.model.Job;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.github.admin.GitHubHookRegisterProblemMonitor;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.kohsuke.github.GHEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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

    @Inject
    private transient GitHubHookRegisterProblemMonitor monitor;

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
     * @param signature request signature, not used on PING events
     */
    @Override
    protected void onEvent(GHEvent event, String payload, String signature) {
        JSONObject parsedPayload = fromObject(payload);
        JSONObject repository = parsedPayload.optJSONObject("repository");
        if (repository != null) {
            LOGGER.info("{} webhook received from repo <{}>!", event, repository.getString("html_url"));
            monitor.resolveProblem(GitHubRepositoryName.create(repository.getString("html_url")));
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

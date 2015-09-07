package org.jenkinsci.plugins.github.webhook.subscriber;

import com.cloudbees.jenkins.GitHubPushTrigger;
import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;
import com.cloudbees.jenkins.GitHubTrigger;
import com.cloudbees.jenkins.GitHubWebHook;
import hudson.Extension;
import hudson.model.Job;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.kohsuke.github.GHEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.collect.Sets.immutableEnumSet;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.triggerFrom;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.withTrigger;
import static org.kohsuke.github.GHEvent.PUSH;

/**
 * By default this plugin interested in push events only when job uses {@link GitHubPushTrigger}
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.12.0
 */
@Extension
@SuppressWarnings("unused")
public class DefaultPushGHEventSubscriber extends GHEventsSubscriber {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPushGHEventSubscriber.class);

    /**
     * This subscriber is applicable only for job with GHPush trigger
     *
     * @param project to check for trigger
     *
     * @return true if project has {@link GitHubPushTrigger}
     */
    @Override
    protected boolean isApplicable(Job<?, ?> project) {
        return withTrigger(GitHubPushTrigger.class).apply(project);
    }

    /**
     * @return set with only push event
     */
    @Override
    protected Set<GHEvent> events() {
        return immutableEnumSet(PUSH);
    }

    /**
     * Calls {@link GitHubPushTrigger} in all projects to handle this hook
     *
     * @param event   only PUSH event
     * @param payload payload of gh-event. Never blank
     */
    @Override
    protected void onEvent(GHEvent event, String payload) {
        JSONObject json = JSONObject.fromObject(payload);
        String repoUrl = json.getJSONObject("repository").getString("url");
        final String pusherName = json.getJSONObject("pusher").getString("name");

        LOGGER.info("Received POST for: {}, pusher: {}", repoUrl, pusherName);
        final GitHubRepositoryName changedRepository = GitHubRepositoryName.create(repoUrl);

        if (changedRepository != null) {
            // run in high privilege to see all the projects anonymous users don't see.
            // this is safe because when we actually schedule a build, it's a build that can
            // happen at some random time anyway.
            ACL.impersonate(ACL.SYSTEM, new Runnable() {
                @Override
                public void run() {
                    for (Job<?, ?> job : Jenkins.getInstance().getAllItems(Job.class)) {
                        GitHubTrigger trigger = triggerFrom(job, GitHubPushTrigger.class);
                        if (trigger != null) {
                            LOGGER.debug("Considering to poke {}", job.getFullDisplayName());
                            if (GitHubRepositoryNameContributor.parseAssociatedNames(job).contains(changedRepository)) {
                                LOGGER.info("Poked {}", job.getFullDisplayName());
                                if (pusherName.equalsIgnoreCase(GitHubPlugin.configuration().getBannedCommitter())) {
                                    LOGGER.info("Skipped {} because user is banned: {}.", job.getFullDisplayName()
                                                                                         , pusherName);
                                } else {
                                    trigger.onPost(pusherName);
                                }
                            } else {
                                LOGGER.debug("Skipped {} because it doesn't have a matching repository.",
                                        job.getFullDisplayName());
                            }
                        }
                    }
                }
            });

            for (GitHubWebHook.Listener listener : Jenkins.getInstance()
                    .getExtensionList(GitHubWebHook.Listener.class)) {
                listener.onPushRepositoryChanged(pusherName, changedRepository);
            }

        } else {
            LOGGER.warn("Malformed repo url {}", repoUrl);
        }
    }
}

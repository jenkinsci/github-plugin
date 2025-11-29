package org.jenkinsci.plugins.github.webhook.subscriber;

import com.cloudbees.jenkins.GitHubPushTrigger;
import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;
import com.cloudbees.jenkins.GitHubTriggerEvent;
import com.cloudbees.jenkins.GitHubWebHook;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Item;
import hudson.security.ACL;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GitHub;
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
    protected boolean isApplicable(Item project) {
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
     */
    @Override
    protected void onEvent(final GHSubscriberEvent event) {
        GHEventPayload.Push push;
        try {
            push = GitHub.offline().parseEventPayload(new StringReader(event.getPayload()), GHEventPayload.Push.class);
        } catch (IOException e) {
            LOGGER.warn("Received malformed PushEvent: " + event.getPayload(), e);
            return;
        }
        URL htmlUrl = push.getRepository().getHtmlUrl();
        final String pusherName = push.getPusher().getName();
        LOGGER.info("Received PushEvent for {} from {}", htmlUrl, event.getOrigin());
        final GitHubRepositoryName changedRepository = GitHubRepositoryName.create(htmlUrl.toExternalForm());

        if (changedRepository != null) {
            // run in high privilege to see all the projects anonymous users don't see.
            // this is safe because when we actually schedule a build, it's a build that can
            // happen at some random time anyway.
            ACL.impersonate(ACL.SYSTEM, new Runnable() {
                @Override
                public void run() {
                    for (Item job : Jenkins.getInstance().getAllItems(Item.class)) {
                        GitHubPushTrigger trigger = triggerFrom(job, GitHubPushTrigger.class);
                        if (trigger != null) {
                            String fullDisplayName = job.getFullDisplayName();
                            LOGGER.debug("Considering to poke {}", fullDisplayName);
                            if (GitHubRepositoryNameContributor.parseAssociatedNames(job)
                                    .contains(changedRepository)) {
                                if (trigger.isUserIgnored(pusherName)) {
                                    LOGGER.debug("Skipped {} because pusher '{}' is in the ignored users list",
                                            fullDisplayName, pusherName);
                                } else {
                                    LOGGER.info("Poked {}", fullDisplayName);
                                    trigger.onPost(GitHubTriggerEvent.create()
                                            .withTimestamp(event.getTimestamp())
                                            .withOrigin(event.getOrigin())
                                            .withTriggeredByUser(pusherName)
                                            .build()
                                    );
                                }
                            } else {
                                LOGGER.debug("Skipped {} because it doesn't have a matching repository.",
                                        fullDisplayName);
                            }
                        }
                    }
                }
            });

            for (GitHubWebHook.Listener listener : ExtensionList.lookup(GitHubWebHook.Listener.class)) {
                listener.onPushRepositoryChanged(pusherName, changedRepository);
            }

        } else {
            LOGGER.warn("Malformed repo html url {}", htmlUrl);
        }
    }
}

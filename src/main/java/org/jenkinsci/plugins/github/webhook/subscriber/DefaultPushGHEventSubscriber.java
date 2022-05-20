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
@SuppressWarnings("deprecation")
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
        URL repoUrl = push.getRepository().getUrl();
        final String pusherName = push.getPusher().getName();
        LOGGER.info("Received PushEvent for {} from {}", repoUrl, event.getOrigin());
        GitHubRepositoryName fromEventRepository = GitHubRepositoryName.create(repoUrl.toExternalForm());

        if (fromEventRepository == null) {
            // On push event on github.com url === html_url
            // this is not consistent with the API docs and with hosted repositories
            // see https://goo.gl/c1qmY7
            // let's retry with 'html_url'
            URL htmlUrl = push.getRepository().getHtmlUrl();
            fromEventRepository = GitHubRepositoryName.create(htmlUrl.toExternalForm());
            if (fromEventRepository != null) {
                LOGGER.debug("PushEvent handling: 'html_url' field "
                        + "has been used to retrieve project information (instead of default 'url' field)");
            }
        }

        final GitHubRepositoryName changedRepository = fromEventRepository;
        LOGGER.info("changedRepository: " + changedRepository.toString());
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
                                LOGGER.info("Poked {}", fullDisplayName);
                                trigger.onPost(GitHubTriggerEvent.create()
                                        .withTimestamp(event.getTimestamp())
                                        .withOrigin(event.getOrigin())
                                        .withTriggeredByUser(pusherName)
                                        .build()
                                );
                            } else {
                                LOGGER.debug("Skipped {} because it doesn't have a matching repository.",
                                        fullDisplayName);
                                LOGGER.debug(GitHubRepositoryNameContributor.parseAssociatedNames(job).toString());
                            }
                        }
                    }
                }
            });

            for (GitHubWebHook.Listener listener : ExtensionList.lookup(GitHubWebHook.Listener.class)) {
                listener.onPushRepositoryChanged(pusherName, changedRepository);
            }

        } else {
            LOGGER.warn("Malformed repo url {}", repoUrl);
        }
    }
}

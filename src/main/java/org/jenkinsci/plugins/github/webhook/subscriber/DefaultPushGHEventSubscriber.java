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
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.kohsuke.github.GHEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern REPOSITORY_NAME_PATTERN = Pattern.compile("https?://([^/]+)/([^/]+)/([^/]+)");

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
        // something like 'https://github.com/bar/foo'
        String repoUrl = json.getJSONObject("repository").getString("url");
        final JSONObject pusher = json.getJSONObject("pusher");
        final String pusherName = pusher.getString("name");

        LOGGER.info("Received POST for {}", repoUrl);
        Matcher matcher = REPOSITORY_NAME_PATTERN.matcher(repoUrl);
        if (matcher.matches()) {
            final GitHubRepositoryName changedRepository = GitHubRepositoryName.create(repoUrl);
            if (changedRepository == null) {
                LOGGER.warn("Malformed repo url {}", repoUrl);
                return;
            }

            // run in high privilege to see all the projects anonymous users don't see.
            // this is safe because when we actually schedule a build, it's a build that can
            // happen at some random time anyway.
            ACL.impersonate(ACL.SYSTEM, new Runnable() {
                @Override
                public void run() {
                    final String pusherEmail = pusher.getString("email");
                    for (Job<?, ?> job : Jenkins.getInstance().getAllItems(Job.class)) {
                        GitHubTrigger trigger = triggerFrom(job, GitHubPushTrigger.class);
                        if (trigger != null) {
                            if (trigger instanceof GitHubPushTrigger) {
                                final String regex = ((GitHubPushTrigger) trigger).getIgnorablePusher();
                                if (regex != null) {
                                    if (pusherName != null && pusherName.matches(regex)) {
                                        LOGGER.info("Ignoring pusher [{}] ...", pusherName);
                                        continue;
                                    }
                                    if (pusherEmail != null && pusherEmail.matches(regex)) {
                                        LOGGER.info("Ignoring pusher [{}] ...", pusherEmail);
                                        continue;
                                    }
                                }
                            }
                            LOGGER.debug("Considering to poke {}", job.getFullDisplayName());
                            if (GitHubRepositoryNameContributor.parseAssociatedNames(job).contains(changedRepository)) {
                                LOGGER.info("Poked {}", job.getFullDisplayName());
                                trigger.onPost(pusherName);
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

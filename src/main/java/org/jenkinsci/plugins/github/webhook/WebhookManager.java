package org.jenkinsci.plugins.github.webhook;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import hudson.model.AbstractProject;
import org.apache.commons.lang.Validate;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHException;
import org.kohsuke.github.GHHook;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.cloudbees.jenkins.GitHubRepositoryNameContributor.parseAssociatedNames;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.base.Predicates.or;
import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.isEqualCollection;
import static org.jenkinsci.plugins.github.extension.GHEventsSubscriber.extractEvents;
import static org.jenkinsci.plugins.github.extension.GHEventsSubscriber.isApplicableFor;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * Class to incapsulate manipulation with webhooks on GH
 * Each manager works with only one hook url (created with {@link #forHookUrl(URL)})
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.12.0
 */
public class WebhookManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookManager.class);

    private final URL endpoint;

    /**
     * Use {@link #forHookUrl(URL)} to create new one
     *
     * @param endpoint url which will be created as hook on GH
     */
    private WebhookManager(URL endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @see #WebhookManager(URL)
     */
    public static WebhookManager forHookUrl(URL endpoint) {
        return new WebhookManager(endpoint);
    }

    /**
     * Creates runnable with ability to create hooks for given project
     * For each GH repo name contributed by {@link com.cloudbees.jenkins.GitHubRepositoryNameContributor},
     * this runnable creates hook (with clean old one).
     *
     * Hook events job interested in, contributes to full set instances of {@link GHEventsSubscriber}.
     * New events will be merged with old ones from existent hook.
     *
     * By default only push event is registered
     *
     * @param project to find for which repos we should create hooks
     *
     * @return runnable to create hooks on run
     * @see #createHookSubscribedTo(List)
     */
    public Runnable registerFor(final AbstractProject<?, ?> project) {
        final Collection<GitHubRepositoryName> names = parseAssociatedNames(project);

        final List<GHEvent> events = from(GHEventsSubscriber.all())
                .filter(isApplicableFor(project))
                .transformAndConcat(extractEvents()).toList();

        return new Runnable() {
            public void run() {
                LOGGER.info("GitHub webhooks activated for job {} with {} (events: {})",
                        project.getFullName(), names, events);

                from(names)
                        .transform(createHookSubscribedTo(events))
                        .filter(notNull())
                        .filter(log("Created hook")).toList();
            }
        };
    }

    /**
     * Used to cleanup old hooks in case of removed or reconfigured trigger
     * since JENKINS-28138 this method permanently removes service hooks
     *
     * So if the trigger for given name was only reconfigured, this method filters only service hooks
     * (with help of aliveRepos names list), otherwise this method removes all hooks for managed url
     *
     * @param name       repository to clean hooks
     * @param aliveRepos repository list which has enabled trigger in jobs
     */
    public void unregisterFor(GitHubRepositoryName name, List<GitHubRepositoryName> aliveRepos) {
        try {
            GHRepository repo = checkNotNull(
                    from(name.resolve()).firstMatch(withAdminAccess()).orNull(),
                    "There is no admin access to manage hooks on %s", name
            );

            LOGGER.debug("Check {} for redundant hooks...", repo);

            Predicate<GHHook> predicate = aliveRepos.contains(name)
                    ? serviceWebhookFor(endpoint) // permanently clear service hooks (JENKINS-28138)
                    : or(serviceWebhookFor(endpoint), webhookFor(endpoint));

            from(fetchHooks().apply(repo))
                    .filter(predicate)
                    .filter(deleteWebhook())
                    .filter(log("Deleted hook")).toList();

        } catch (Throwable t) {
            LOGGER.warn("Failed to remove hook from {}", name, t);
        }
    }

    /**
     * Main logic of {@link #registerFor(AbstractProject)}.
     * Updates hooks with replacing old ones with merged new ones
     *
     * @param events calculated events list to be registered in hook
     *
     * @return function to register hooks for given events
     */
    protected Function<GitHubRepositoryName, GHHook> createHookSubscribedTo(final List<GHEvent> events) {
        return new Function<GitHubRepositoryName, GHHook>() {
            @Override
            public GHHook apply(GitHubRepositoryName name) {
                try {
                    GHRepository repo = checkNotNull(
                            from(name.resolve()).firstMatch(withAdminAccess()).orNull(),
                            "There is no admin access to manage hooks on %s", name
                    );

                    Validate.notEmpty(events, "Events list for hook can't be empty");

                    Set<GHHook> hooks = from(fetchHooks().apply(repo))
                            .filter(webhookFor(endpoint))
                            .toSet();

                    Set<GHEvent> alreadyRegistered = from(hooks)
                            .transformAndConcat(eventsFromHook()).toSet();

                    if (hooks.size() == 1 && isEqualCollection(alreadyRegistered, events)) {
                        LOGGER.debug("Hook already registered for events {}", events);
                        return null;
                    }

                    Set<GHEvent> merged = from(alreadyRegistered).append(events).toSet();

                    from(hooks)
                            .filter(deleteWebhook())
                            .filter(log("Replaced hook")).toList();

                    return createWebhook(endpoint, merged).apply(repo);
                } catch (Throwable t) {
                    LOGGER.warn("Failed to add GitHub webhook for {}", name, t);
                }
                return null;
            }
        };
    }

    /**
     * Mostly debug method. Logs hook manipulation result
     *
     * @param format prepended comment for log
     *
     * @return always true predicate
     */
    private Predicate<GHHook> log(final String format) {
        return new Predicate<GHHook>() {
            @Override
            public boolean apply(GHHook input) {
                LOGGER.debug(format("%s {} (events: {})", format), input.getUrl(), input.getEvents());
                return true;
            }
        };
    }

    /**
     * Filters repos with admin rights (to manage hooks)
     *
     * @return true if we have admin rights for repo
     */
    protected Predicate<GHRepository> withAdminAccess() {
        return new Predicate<GHRepository>() {
            @Override
            public boolean apply(GHRepository repo) {
                return repo.hasAdminAccess();
            }
        };
    }

    /**
     * Finds "Jenkins (GitHub)" service webhook
     *
     * @param url jenkins endpoint url
     *
     * @return true if hook is service hook
     */
    protected Predicate<GHHook> serviceWebhookFor(final URL url) {
        return new Predicate<GHHook>() {
            public boolean apply(GHHook hook) {
                return hook.getName().equals("jenkins")
                        && hook.getConfig().get("jenkins_hook_url").equals(url.toExternalForm());
            }
        };
    }

    /**
     * Finds hook with endpoint url
     *
     * @param url jenkins endpoint url
     *
     * @return true if hook is standard webhook
     */
    protected Predicate<GHHook> webhookFor(final URL url) {
        return new Predicate<GHHook>() {
            public boolean apply(GHHook hook) {
                return hook.getName().equals("web")
                        && hook.getConfig().get("url").equals(url.toExternalForm());
            }
        };
    }

    /**
     * @return converter to extract events from each hook
     */
    protected Function<GHHook, Iterable<GHEvent>> eventsFromHook() {
        return new Function<GHHook, Iterable<GHEvent>>() {
            @Override
            public Iterable<GHEvent> apply(GHHook input) {
                return input.getEvents();
            }
        };
    }
    
    /*
     * ACTIONS
     */

    /**
     * @return converter to fetch from GH hooks list for each repo
     */
    protected Function<GHRepository, List<GHHook>> fetchHooks() {
        return new Function<GHRepository, List<GHHook>>() {
            @Override
            public List<GHHook> apply(GHRepository repo) {
                try {
                    return repo.getHooks();
                } catch (IOException e) {
                    throw new GHException("Failed to fetch post-commit hooks", e);
                }
            }
        };
    }

    /**
     * @param url    jenkins endpoint url
     * @param events list of GH events jenkins interested in
     *
     * @return converter to create GH hook for given url with given events
     */
    protected Function<GHRepository, GHHook> createWebhook(final URL url, final Set<GHEvent> events) {
        return new Function<GHRepository, GHHook>() {
            public GHHook apply(GHRepository repo) {
                try {
                    return repo.createWebHook(url, events);
                } catch (IOException e) {
                    throw new GHException("Failed to create hook", e);
                }
            }
        };
    }

    /**
     * @return annihilator for hook, returns true if deletion was successful
     */
    protected Predicate<GHHook> deleteWebhook() {
        return new Predicate<GHHook>() {
            public boolean apply(GHHook hook) {
                try {
                    hook.delete();
                    return true;
                } catch (IOException e) {
                    throw new GHException("Failed to delete post-commit hook", e);
                }
            }
        };
    }
}

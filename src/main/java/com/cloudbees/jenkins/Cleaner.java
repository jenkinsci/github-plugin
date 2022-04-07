package com.cloudbees.jenkins;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.PeriodicWork;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.webhook.WebhookManager;

import java.net.URL;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.associatedNames;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isAlive;

/**
 * Removes post-commit hooks from repositories that we no longer care.
 *
 * This runs periodically in a delayed fashion to avoid hitting GitHub too often.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class Cleaner extends PeriodicWork {
    /**
     * Queue contains repo names prepared to cleanup.
     * After configure method on job, trigger calls {@link #onStop(Item)}
     * which converts to repo names with help of contributors.
     *
     * This queue is thread-safe, so any thread can write or
     * fetch names to this queue without additional sync
     */
    private final Queue<GitHubRepositoryName> cleanQueue = new ConcurrentLinkedQueue<>();

    /**
     * Called when a {@link GitHubPushTrigger} is about to be removed.
     */
    /* package */ void onStop(Item item) {
        cleanQueue.addAll(GitHubRepositoryNameContributor.parseAssociatedNames(item));
    }

    @Override
    public long getRecurrencePeriod() {
        return TimeUnit.MINUTES.toMillis(3);
    }

    /**
     * Each run this work fetches alive repo names (which has trigger for it)
     * then if names queue is not empty (any job was reconfigured with GH trigger change),
     * next name passed to {@link WebhookManager} with list of active names to check and unregister old hooks
     */
    @Override
    protected void doRun() throws Exception {
        if (!GitHubPlugin.configuration().isManageHooks()) {
            return;
        }

        URL url = GitHubPlugin.configuration().getHookUrl();

        List<GitHubRepositoryName> aliveRepos = from(Jenkins.get().allItems(Item.class))
                .filter(isAlive())  // live repos
                .transformAndConcat(associatedNames()).toList();

        while (!cleanQueue.isEmpty()) {
            GitHubRepositoryName name = cleanQueue.poll();

            WebhookManager.forHookUrl(url).unregisterFor(name, aliveRepos);
        }
    }

    public static Cleaner get() {
        return PeriodicWork.all().get(Cleaner.class);
    }
}

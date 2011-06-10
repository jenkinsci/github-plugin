package com.cloudbees.jenkins;

import com.cloudbees.jenkins.GitHubPushTrigger.DescriptorImpl;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.PeriodicWork;
import hudson.triggers.Trigger;
import hudson.util.TimeUnit2;
import org.kohsuke.github.GHRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Removes post-commit hooks from repositories that we no longer care.
 *
 * This runs periodically in a delayed fashion to avoid hitting GitHub too often.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class Cleaner extends PeriodicWork {
    private final Set<GitHubRepositoryName> couldHaveBeenRemoved = new HashSet<GitHubRepositoryName>();

    /**
     * Called when a {@link GitHubPushTrigger} is about to be removed.
     */
    synchronized void onStop(GitHubPushTrigger trigger) {
        couldHaveBeenRemoved.addAll(trigger.getGitHubRepositories());
    }

    @Override
    public long getRecurrencePeriod() {
        return TimeUnit2.MINUTES.toMillis(3);
    }

    @Override
    protected void doRun() throws Exception {
        List<GitHubRepositoryName> names;
        synchronized (this) {// atomically obtain what we need to check
            names = new ArrayList<GitHubRepositoryName>(couldHaveBeenRemoved);
            couldHaveBeenRemoved.clear();
        }

        // subtract all the live repositories
        for (AbstractProject<?,?> job : Hudson.getInstance().getItems(AbstractProject.class)) {
            GitHubPushTrigger trigger = job.getTrigger(GitHubPushTrigger.class);
            if (trigger!=null) {
                names.removeAll(trigger.getGitHubRepositories());
            }
        }

        // these are the repos that we are no longer interested.
        // erase our hooks
        OUTER:
        for (GitHubRepositoryName r : names) {
            for (GHRepository repo : r.resolve()) {
                try {
                    repo.getPostCommitHooks().remove(
                        Trigger.all().get(DescriptorImpl.class).getHookUrl());
                    LOGGER.fine("Removed a hook from "+r+"");
                    continue OUTER;
                } catch (Throwable e) {
                    LOGGER.log(Level.WARNING,"Failed to remove hook from "+r,e);
                }
            }
        }
    }

    public static Cleaner get() {
        return PeriodicWork.all().get(Cleaner.class);
    }

    private static final Logger LOGGER = Logger.getLogger(Cleaner.class.getName());
}

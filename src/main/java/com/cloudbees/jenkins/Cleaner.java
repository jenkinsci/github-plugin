package com.cloudbees.jenkins;

import com.cloudbees.jenkins.GitHubPushTrigger.DescriptorImpl;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.PeriodicWork;
import hudson.triggers.Trigger;
import hudson.util.TimeUnit2;

import org.kohsuke.github.GHException;
import org.kohsuke.github.GHHook;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;

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
    synchronized void onStop(Job<?,?> job) {
        couldHaveBeenRemoved.addAll(GitHubRepositoryNameContributor.parseAssociatedNames(job));
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
        for (Job<?,?> job : getActiveInstance().getAllItems(Job.class)) {
            ParameterizedJobMixIn.ParameterizedJob pJob = (ParameterizedJobMixIn.ParameterizedJob) job;
            for (Trigger trigger : pJob.getTriggers().values()) {
                if (trigger instanceof GitHubPushTrigger) {
                    names.removeAll(GitHubRepositoryNameContributor.parseAssociatedNames(job));
                    break;
                }
            }
        }

        // these are the repos that we are no longer interested.
        // erase our hooks
        OUTER:
        for (GitHubRepositoryName r : names) {
            for (GHRepository repo : r.resolve()) {
                try {
                    removeHook(repo, Trigger.all().get(DescriptorImpl.class).getHookUrl());
                    LOGGER.info("Removed a hook from "+r+"");
                    continue OUTER;
                } catch (Throwable e) {
                    LOGGER.log(Level.WARNING,"Failed to remove hook from "+r,e);
                }
            }
        }
    }

    //Maybe we should create a remove hook method in the Github API
    //something like public void removeHook(String name, Map<String,String> config)
    private void removeHook(GHRepository repo, URL url) {
        try {
            String urlExternalForm = url.toExternalForm();
            for (GHHook h : repo.getHooks()) {
                if (h.getName().equals("jenkins") && h.getConfig().get("jenkins_hook_url").equals(urlExternalForm)) {
                    h.delete();
                }
            }
        } catch (IOException e) {
            throw new GHException("Failed to update post-commit hooks", e);
        }
    }

    public static Cleaner get() {
        return PeriodicWork.all().get(Cleaner.class);
    }

    private static final Logger LOGGER = Logger.getLogger(Cleaner.class.getName());

    // TODO: this method will not be required after upgrading core to 1.596. Call Jenkins.getActiveInstance() directly
    private static Jenkins getActiveInstance() {
        Jenkins instance = Jenkins.getInstance();
        if (instance == null) {
            throw new IllegalStateException("Jenkins has not been started, or was already shut down");
        }
        return instance;
    }
}

package org.jenkinsci.plugins.github.util;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.triggers.Trigger;
import jenkins.model.ParameterizedJobMixIn;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;

import javax.annotation.CheckForNull;
import java.util.Collection;

import static org.jenkinsci.plugins.github.extension.GHEventsSubscriber.isApplicableFor;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * Utility class which holds converters or predicates (matchers) to filter or convert job lists
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.12.0
 */
public final class JobInfoHelpers {

    private JobInfoHelpers() {
        throw new IllegalAccessError("Do not instantiate it");
    }

    /**
     * @param clazz trigger class to check in job
     *
     * @return predicate with true on apply if job contains trigger of given class
     */
    public static Predicate<Job> withTrigger(final Class<? extends Trigger> clazz) {
        return new Predicate<Job>() {
            public boolean apply(Job job) {
                return triggerFrom(job, clazz) != null;
            }
        };
    }

    /**
     * Can be useful to ignore disabled jobs on reregistering hooks
     *
     * @return predicate with true on apply if job is buildable
     */
    public static Predicate<Job> isBuildable() {
        return new Predicate<Job>() {
            public boolean apply(Job job) {
                return job != null && job.isBuildable();
            }
        };
    }

    /**
     * @return function which helps to convert job to repo names associated with this job
     */
    public static Function<Job, Collection<GitHubRepositoryName>> associatedNames() {
        return new Function<Job, Collection<GitHubRepositoryName>>() {
            public Collection<GitHubRepositoryName> apply(Job job) {
                return GitHubRepositoryNameContributor.parseAssociatedNames(job);
            }
        };
    }

    /**
     * If any of event subscriber interested in hook for job, then return true
     * By default, push hook subscriber is interested in job with gh-push-trigger
     *
     * @return predicate with true if job alive and should have hook
     */
    public static Predicate<Job> isAlive() {
        return new Predicate<Job>() {
            @Override
            public boolean apply(Job job) {
                return !from(GHEventsSubscriber.all()).filter(isApplicableFor(job)).toList().isEmpty();
            }
        };
    }

    /**
     * @param job    job to search trigger in
     * @param tClass trigger with class which we want to receive from job
     * @param <T>    type of trigger
     *
     * @return Trigger instance with required class or null
     * TODO use standard method in 1.621+
     */
    @CheckForNull
    public static <T extends Trigger> T triggerFrom(Job<?, ?> job, Class<T> tClass) {
        if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
            ParameterizedJobMixIn.ParameterizedJob pJob = (ParameterizedJobMixIn.ParameterizedJob) job;

            for (Trigger candidate : pJob.getTriggers().values()) {
                if (tClass.isInstance(candidate)) {
                    return tClass.cast(candidate);
                }
            }
        }
        return null;
    }

    /**
     * Converts any child class of {@link Job} (such as {@link AbstractProject}
     * to {@link ParameterizedJobMixIn} to use it for workflow
     *
     * @param job to wrap
     * @param <T> any child type of Job
     *
     * @return ParameterizedJobMixIn
     * TODO use standard method in 1.621+
     */
    public static <T extends Job> ParameterizedJobMixIn asParameterizedJobMixIn(final T job) {
        return new ParameterizedJobMixIn() {
            @Override
            protected Job asJob() {
                return job;
            }
        };
    }
}


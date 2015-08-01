package org.jenkinsci.plugins.github.util;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.triggers.Trigger;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;

import java.util.Collection;

import static org.apache.commons.lang3.Validate.notNull;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;
import static org.jenkinsci.plugins.github.extension.GHEventsSubscriber.isApplicableFor;

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
    public static Predicate<AbstractProject> withTrigger(final Class<? extends Trigger> clazz) {
        return new Predicate<AbstractProject>() {
            @Override
            public boolean apply(AbstractProject job) {
                return notNull(job).getTrigger(clazz) != null;
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
            @Override
            public boolean apply(Job job) {
                Validate.notNull(job);
                return  job.isBuildable();
            }
        };
    }

    /**
     * @return function which helps to convert job to repo names associated with this job
     */
    public static Function<AbstractProject, Collection<GitHubRepositoryName>> associatedNames() {
        return new Function<AbstractProject, Collection<GitHubRepositoryName>>() {
            public Collection<GitHubRepositoryName> apply(AbstractProject job) {
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
    public static Predicate<AbstractProject> isAlive() {
        return new Predicate<AbstractProject>() {
            @Override
            public boolean apply(AbstractProject job) {
                return !from(GHEventsSubscriber.all()).filter(isApplicableFor(job)).toList().isEmpty();
            }
        };
    }
}


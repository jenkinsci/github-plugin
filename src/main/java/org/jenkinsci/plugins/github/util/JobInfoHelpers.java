package org.jenkinsci.plugins.github.util;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import hudson.model.AbstractProject;
import hudson.triggers.Trigger;

import java.util.Collection;

/**
 * Utility class which holds converters or predicates (matchers) to filter or convert job lists
 *
 * @author lanwen (Merkushev Kirill)
 * @since TODO
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
            public boolean apply(AbstractProject job) {
                return job.getTrigger(clazz) != null;
            }
        };
    }

    /**
     * Can be useful to ignore disabled jobs on reregistering hooks
     *
     * @return predicate with true on apply if job is buildable
     */
    public static Predicate<AbstractProject> isBuildable() {
        return new Predicate<AbstractProject>() {
            public boolean apply(AbstractProject job) {
                return job.isBuildable();
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
}

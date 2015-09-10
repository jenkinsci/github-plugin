package com.cloudbees.jenkins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.EnvironmentContributor;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import jenkins.model.Jenkins;
import jenkins.triggers.SCMTriggerItem;
import jenkins.triggers.SCMTriggerItem.SCMTriggerItems;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Extension point that associates {@link GitHubRepositoryName}s to a project.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.7
 */
public abstract class GitHubRepositoryNameContributor implements ExtensionPoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubRepositoryNameContributor.class);

    /**
     * Looks at the definition of {@link AbstractProject} and list up the related github repositories,
     * then puts them into the collection.
     *
     * @deprecated Use {@link #parseAssociatedNames(Job, Collection)}
     */
    @Deprecated
    public void parseAssociatedNames(AbstractProject<?, ?> job, Collection<GitHubRepositoryName> result) {
        parseAssociatedNames((Job) job, result);
    }

    /**
     * Looks at the definition of {@link Job} and list up the related github repositories,
     * then puts them into the collection.
     */
    public /*abstract*/ void parseAssociatedNames(Job<?, ?> job, Collection<GitHubRepositoryName> result) {
        if (overriddenMethodHasDeprecatedSignature(job)) {
            parseAssociatedNames((AbstractProject) job, result);
        } else {
            throw new AbstractMethodError("you must override the new overload of parseAssociatedNames");
        }
    }

    /**
     * To select backward compatible method with old extensions
     * with overridden {@link #parseAssociatedNames(AbstractProject, Collection)}
     *
     * @param job - parameter to check for old class
     *
     * @return true if overridden deprecated method
     */
    private boolean overriddenMethodHasDeprecatedSignature(Job<?, ?> job) {
        return Util.isOverridden(
                GitHubRepositoryNameContributor.class,
                getClass(),
                "parseAssociatedNames",
                AbstractProject.class,
                Collection.class
        ) && job instanceof AbstractProject;
    }

    public static ExtensionList<GitHubRepositoryNameContributor> all() {
        return Jenkins.getInstance().getExtensionList(GitHubRepositoryNameContributor.class);
    }

    /**
     * @deprecated Use {@link #parseAssociatedNames(Job)}
     */
    @Deprecated
    public static Collection<GitHubRepositoryName> parseAssociatedNames(AbstractProject<?, ?> job) {
        return parseAssociatedNames((Job) job);
    }

    public static Collection<GitHubRepositoryName> parseAssociatedNames(Job<?, ?> job) {
        Set<GitHubRepositoryName> names = new HashSet<GitHubRepositoryName>();
        for (GitHubRepositoryNameContributor c : all()) {
            c.parseAssociatedNames(job, names);
        }
        return names;
    }

    /**
     * Default implementation that looks at SCMs
     */
    @Extension
    public static class FromSCM extends GitHubRepositoryNameContributor {
        @Override
        public void parseAssociatedNames(Job<?, ?> job, Collection<GitHubRepositoryName> result) {
            SCMTriggerItem item = SCMTriggerItems.asSCMTriggerItem(job);
            EnvVars envVars = buildEnv(job);
            if (item != null) {
                for (SCM scm : item.getSCMs()) {
                    addRepositories(scm, envVars, result);
                }
            }
        }

        protected EnvVars buildEnv(Job<?, ?> job) {
            EnvVars env = new EnvVars();
            for (EnvironmentContributor contributor : EnvironmentContributor.all()) {
                try {
                    contributor.buildEnvironmentFor(job, env, TaskListener.NULL);
                } catch (Exception e) {
                    LOGGER.debug("{} failed to build env ({}), skipping", contributor.getClass(), e.getMessage(), e);
                }
            }
            return env;
        }

        protected static void addRepositories(SCM scm, EnvVars env, Collection<GitHubRepositoryName> r) {
            if (scm instanceof GitSCM) {
                GitSCM git = (GitSCM) scm;
                for (RemoteConfig rc : git.getRepositories()) {
                    for (URIish uri : rc.getURIs()) {
                        String url = env.expand(uri.toString());
                        GitHubRepositoryName repo = GitHubRepositoryName.create(url);
                        if (repo != null) {
                            r.add(repo);
                        }
                    }
                }
            }
        }
    }
}

package com.cloudbees.jenkins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.EnvironmentContributor;
import hudson.model.Item;
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
     * @deprecated Use {@link #parseAssociatedNames(Item, Collection)}
     */
    @Deprecated
    public void parseAssociatedNames(AbstractProject<?, ?> job, Collection<GitHubRepositoryName> result) {
        parseAssociatedNames((Item) job, result);
    }

    /**
     * Looks at the definition of {@link Job} and list up the related github repositories,
     * then puts them into the collection.
     * @deprecated Use {@link #parseAssociatedNames(Item, Collection)}
     */
    @Deprecated
    public /*abstract*/ void parseAssociatedNames(Job<?, ?> job, Collection<GitHubRepositoryName> result) {
        parseAssociatedNames((Item) job, result);
    }

    /**
     * Looks at the definition of {@link Item} and list up the related github repositories,
     * then puts them into the collection.
     * @param item the item.
     * @param result the collection to add repository names to
     * @since FIXME
     */
    @SuppressWarnings("deprecation")
    public /*abstract*/ void parseAssociatedNames(Item item, Collection<GitHubRepositoryName> result) {
        if (Util.isOverridden(
                GitHubRepositoryNameContributor.class,
                getClass(),
                "parseAssociatedNames",
                Job.class,
                Collection.class
        )) {
            // if this impl is legacy, it cannot contribute to non-jobs, so not an error
            if (item instanceof Job) {
                parseAssociatedNames((Job<?, ?>) item, result);
            }
        } else  if (Util.isOverridden(
                GitHubRepositoryNameContributor.class,
                getClass(),
                "parseAssociatedNames",
                AbstractProject.class,
                Collection.class
        )) {
            // if this impl is legacy, it cannot contribute to non-projects, so not an error
            if (item instanceof AbstractProject) {
                parseAssociatedNames((AbstractProject<?, ?>) item, result);
            }
        } else {
            throw new AbstractMethodError("you must override the new overload of parseAssociatedNames");
        }
    }

    public static ExtensionList<GitHubRepositoryNameContributor> all() {
        return Jenkins.getInstance().getExtensionList(GitHubRepositoryNameContributor.class);
    }

    /**
     * @deprecated Use {@link #parseAssociatedNames(Job)}
     */
    @Deprecated
    public static Collection<GitHubRepositoryName> parseAssociatedNames(AbstractProject<?, ?> job) {
        return parseAssociatedNames((Item) job);
    }

    /**
     * @deprecated Use {@link #parseAssociatedNames(Item)}
     */
    @Deprecated
    public static Collection<GitHubRepositoryName> parseAssociatedNames(Job<?, ?> job) {
        return parseAssociatedNames((Item) job);
    }

    public static Collection<GitHubRepositoryName> parseAssociatedNames(Item item) {
        Set<GitHubRepositoryName> names = new HashSet<GitHubRepositoryName>();
        for (GitHubRepositoryNameContributor c : all()) {
            c.parseAssociatedNames(item, names);
        }
        return names;
    }

    /**
     * Default implementation that looks at SCMs
     */
    @Extension
    public static class FromSCM extends GitHubRepositoryNameContributor {
        @Override
        public void parseAssociatedNames(Item item, Collection<GitHubRepositoryName> result) {
            SCMTriggerItem triggerItem = SCMTriggerItems.asSCMTriggerItem(item);
            EnvVars envVars = item instanceof Job ? buildEnv((Job) item) : new EnvVars();
            if (triggerItem != null) {
                for (SCM scm : triggerItem.getSCMs()) {
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

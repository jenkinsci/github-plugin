package com.cloudbees.jenkins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
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
import org.jenkinsci.plugins.multiplescms.MultiSCM;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extension point that associates {@link GitHubRepositoryName}s to a project.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.7
 */
public abstract class GitHubRepositoryNameContributor implements ExtensionPoint {
    /**
     * Looks at the definition of {@link AbstractProject} and list up the related github repositories,
     * then puts them into the collection.
     */
    public abstract void parseAssociatedNames(Job<?,?> job, Collection<GitHubRepositoryName> result);

    public static ExtensionList<GitHubRepositoryNameContributor> all() {
        return Jenkins.getInstance().getExtensionList(GitHubRepositoryNameContributor.class);
    }

    public static Collection<GitHubRepositoryName> parseAssociatedNames(Job<?, ?> job) {
        Set<GitHubRepositoryName> names = new HashSet<GitHubRepositoryName>();
        for (GitHubRepositoryNameContributor c : all()) {
            c.parseAssociatedNames(job, names);
        }
        return names;
    }


    static abstract class AbstractFromSCMImpl extends GitHubRepositoryNameContributor {
        protected EnvVars buildEnv(Job<?, ?> job) {
            EnvVars env = new EnvVars();
            for (EnvironmentContributor contributor : EnvironmentContributor.all()) {
                try {
                    contributor.buildEnvironmentFor(job, env, TaskListener.NULL);
                } catch (Exception e) {
                    // ignore
                }
            }
            return env;
        }

        protected static void addRepositories(Collection<? extends SCM> scms, EnvVars env, Collection<GitHubRepositoryName> r) {
            for(SCM scm : scms) {
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

    /**
     * Default implementation that looks at SCM
     */
    @Extension
    public static class FromSCM extends AbstractFromSCMImpl {
        @Override
        public void parseAssociatedNames(Job<?, ?> job, Collection<GitHubRepositoryName> result) {
            SCMTriggerItem item = SCMTriggerItems.asSCMTriggerItem(job);
            addRepositories(item.getSCMs(), buildEnv(job), result);
        }
    }

    /**
     * MultiSCM support separated into a different extension point since this is an optional dependency
     */
    @Extension(optional=true)
    public static class FromMultiSCM extends AbstractFromSCMImpl {
        // make this class fail to load if MultiSCM is not present
        public FromMultiSCM() { MultiSCM.class.toString(); }

        @Override
        public void parseAssociatedNames(Job<?, ?> job, Collection<GitHubRepositoryName> result) {
            SCMTriggerItem item = SCMTriggerItems.asSCMTriggerItem(job);
            EnvVars env = buildEnv(job);
            addRepositories(item.getSCMs(), env, result);
        }
    }
}

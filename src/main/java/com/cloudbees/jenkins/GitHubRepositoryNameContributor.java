package com.cloudbees.jenkins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractProject;
import hudson.model.EnvironmentContributor;
import hudson.model.TaskListener;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import jenkins.model.Jenkins;

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
    public abstract void parseAssociatedNames(AbstractProject<?,?> job, Collection<GitHubRepositoryName> result);

    /**
     * Looks at the definition of {@link AbstractProject} and list up its branches, then puts them into
     * the collection.
     */
    public abstract void parseAssociatedBranches(AbstractProject<?,?> job, Collection<GitHubBranch> result);

    public static ExtensionList<GitHubRepositoryNameContributor> all() {
        return Jenkins.getInstance().getExtensionList(GitHubRepositoryNameContributor.class);
    }

    public static Collection<GitHubRepositoryName> parseAssociatedNames(AbstractProject<?,?> job) {
        Set<GitHubRepositoryName> names = new HashSet<GitHubRepositoryName>();
        for (GitHubRepositoryNameContributor c : all())
            c.parseAssociatedNames(job,names);
        return names;
    }

    public static Collection<GitHubBranch> parseAssociatedBranches(AbstractProject<?,?> job) {
        Set<GitHubBranch> names = new HashSet<GitHubBranch>();
        for (GitHubRepositoryNameContributor c : all())
            c.parseAssociatedBranches(job,names);
        return names;
    }

    static abstract class AbstractFromSCMImpl extends GitHubRepositoryNameContributor {
        protected EnvVars buildEnv(AbstractProject<?, ?> job) {
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

        protected static void addBranches(SCM scm, Collection<GitHubBranch> r) {
            if (scm instanceof GitSCM) {
                GitSCM git = (GitSCM) scm;
                for (BranchSpec branch : git.getBranches()) {
                    GitHubBranch gitHubBranch = GitHubBranch.create(branch);
                    if (gitHubBranch != null) {
                        r.add(gitHubBranch);
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
        public void parseAssociatedNames(AbstractProject<?, ?> job, Collection<GitHubRepositoryName> result) {
            addRepositories(job.getScm(), buildEnv(job), result);
        }

        @Override
        public void parseAssociatedBranches(AbstractProject<?, ?> job,
                Collection<GitHubBranch> result) {
            addBranches(job.getScm(), result);
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
        public void parseAssociatedNames(AbstractProject<?, ?> job, Collection<GitHubRepositoryName> result) {
            if (job.getScm() instanceof MultiSCM) {
                EnvVars env = buildEnv(job);

                MultiSCM multiSCM = (MultiSCM) job.getScm();
                List<SCM> scmList = multiSCM.getConfiguredSCMs();
                for (SCM scm : scmList) {
                    addRepositories(scm, env, result);
                }
            }
        }

        @Override
        public void parseAssociatedBranches(AbstractProject<?, ?> job, Collection<GitHubBranch> result) {
            if (Jenkins.getInstance().getPlugin("multiple-scms") != null
                    && job.getScm() instanceof MultiSCM) {
                MultiSCM multiSCM = (MultiSCM) job.getScm();
                List<SCM> scmList = multiSCM.getConfiguredSCMs();
                for (SCM scm : scmList) {
                    addBranches(scm, result);
                }
            } else {
                addBranches(job.getScm(), result);
            }
        }
    }
}

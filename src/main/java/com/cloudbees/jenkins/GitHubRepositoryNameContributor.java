package com.cloudbees.jenkins;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractProject;
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

    public static ExtensionList<GitHubRepositoryNameContributor> all() {
        return Jenkins.getInstance().getExtensionList(GitHubRepositoryNameContributor.class);
    }

    public static Collection<GitHubRepositoryName> parseAssociatedNames(AbstractProject<?,?> job) {
        Set<GitHubRepositoryName> names = new HashSet<GitHubRepositoryName>();
        for (GitHubRepositoryNameContributor c : all())
            c.parseAssociatedNames(job,names);
        return names;
    }


    /**
     * Default implementation that looks at SCM
     */
    @Extension
    public static class FromSCM extends GitHubRepositoryNameContributor {
        @Override
        public void parseAssociatedNames(AbstractProject<?, ?> job, Collection<GitHubRepositoryName> result) {
            if (Jenkins.getInstance().getPlugin("multiple-scms") != null
                    && job.getScm() instanceof MultiSCM) {
                MultiSCM multiSCM = (MultiSCM) job.getScm();
                List<SCM> scmList = multiSCM.getConfiguredSCMs();
                for (SCM scm : scmList) {
                    addRepositories(scm, result);
                }
            } else {
                addRepositories(job.getScm(), result);
            }
        }

        private void addRepositories(SCM scm, Collection<GitHubRepositoryName> r) {
            if (scm instanceof GitSCM) {
                GitSCM git = (GitSCM) scm;
                for (RemoteConfig rc : git.getRepositories()) {
                    for (URIish uri : rc.getURIs()) {
                        String url = uri.toString();
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

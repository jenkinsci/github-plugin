package com.cloudbees.jenkins;

import hudson.util.AdaptedIterator;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Uniquely identifies a repository on GitHub.
 *
 * @author Kohsuke Kawaguchi
 */
public class GitHubRepositoryName {
    public final String host, userName, repositoryName;

    public GitHubRepositoryName(String host, String userName, String repositoryName) {
        this.host = host;
        this.userName = userName;
        this.repositoryName = repositoryName;
    }

    public Iterable<GHRepository> resolve() {
        return new Iterable<GHRepository>() {
            public Iterator<GHRepository> iterator() {
                return new AdaptedIterator<GitHub,GHRepository>(GitHubWebHook.get().login(host,userName)) {
                    protected GHRepository adapt(GitHub item) {
                        try {
                            return item.getUser(userName).getRepository(repositoryName);
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING,"Failed to obtain repository "+this,e);
                            return null;
                        }
                    }
                };
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GitHubRepositoryName that = (GitHubRepositoryName) o;

        return repositoryName.equals(that.repositoryName) && userName.equals(that.userName) && host.equals(that.host);

    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] {host, userName, repositoryName});
    }

    @Override
    public String toString() {
        return "GitHubRepository[host="+host+",username="+userName+",repository="+repositoryName+"]";
    }

    private static final Logger LOGGER = Logger.getLogger(GitHubRepositoryName.class.getName());
}

package com.cloudbees.jenkins;

import hudson.util.AdaptedIterator;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uniquely identifies a repository on GitHub.
 *
 * @author Kohsuke Kawaguchi
 */
public class GitHubRepositoryName {

    private static final Pattern[] URL_PATTERNS = {
        Pattern.compile("git@(.+):([^/]+)/([^/]+).git"),
        Pattern.compile("https://[^/]+@([^/]+)/([^/]+)/([^/]+).git"),
        Pattern.compile("git://([^/]+)/([^/]+)/([^/]+).git"),
        Pattern.compile("ssh://git@([^/]+)/([^/]+)/([^/]+).git")
    };

    /**
     * Create {@link GitHubRepositoryName} from URL
     * 
     * @param url
     *            must be non-null
     * @return parsed {@link GitHubRepositoryName} or null if it cannot be
     *         parsed from the specified URL
     */
    public static GitHubRepositoryName create(final String url) {
        for (Pattern p : URL_PATTERNS) {
            Matcher m = p.matcher(url);
            if (m.matches())
                return new GitHubRepositoryName(m.group(1), m.group(2),
                        m.group(3));
        }
        return null;
    }

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
                            GHRepository repo = item.getUser(userName).getRepository(repositoryName);
                            if (repo == null) {
                                repo = item.getOrganization(userName).getRepository(repositoryName);
                            }
                            return repo;
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

package com.cloudbees.jenkins;

import hudson.util.AdaptedIterator;
import hudson.util.Iterators.FilterIterator;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPerson;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
        Pattern.compile("git@(.+):([^/]+)/([^/]+)(\\.git|)"),
        Pattern.compile("https://[^/]+@([^/]+)/([^/]+)/([^/]+)(\\.git|)"),
        Pattern.compile("https://([^/]+)/([^/]+)/([^/]+)(\\.git|)"),
        Pattern.compile("git://([^/]+)/([^/]+)/([^/]+)(\\.git|)"),
        Pattern.compile("ssh://git@([^/]+)/([^/]+)/([^/]+)(\\.git|)")
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

    /**
     * Resolves this name to the actual reference by {@link GHRepository}.
     *
     * <p>
     * Since the system can store multiple credentials, and only some of them might be able to see this name in question,
     * this method uses {@link GitHubWebHook#login(String, String)} and attempt to find the right credential that can
     * access this repository.
     *
     * <p>
     * This method walks multiple repositories for each credential that can access the repository. Depending on
     * what you are trying to do with the repository, you might have to keep trying until a {@link GHRepository}
     * with suitable permission is returned.
     */
    public Iterable<GHRepository> resolve() {
        return new Iterable<GHRepository>() {
            public Iterator<GHRepository> iterator() {
                return filterNull(new AdaptedIterator<GitHub,GHRepository>(GitHubWebHook.get().login(host,userName)) {
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
                });
            }
        };
    }

    /**
     * Variation of {@link #resolve()} method that just returns the first valid repository object.
     *
     * This is useful if the caller only relies on the read access to the repository and doesn't need to
     * walk possible candidates.
     */
    public GHRepository resolveOne() {
        for (GHRepository r : resolve())
            return r;
        return null;
    }

    private <V> Iterator<V> filterNull(Iterator<V> itr) {
        return new FilterIterator<V>(itr) {
            @Override
            protected boolean filter(V v) {
                return v!=null;
            }
        };
    }

    /**
     * Does this repository match the repository referenced in the given {@link GHCommitPointer}?
     */
    public boolean matches(GHCommitPointer commit) {
        try {
            return userName.equals(commit.getUser().getLogin())
                && repositoryName.equals(commit.getRepository().getName())
                && host.equals(new URL(commit.getRepository().getUrl()).getHost());
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * Does this repository match the repository referenced in the given {@link GHCommitPointer}?
     */
    public boolean matches(GHRepository repo) throws IOException {
        return userName.equals(repo.getOwner().getLogin()) // TODO: use getOwnerName
            && repositoryName.equals(repo.getName())
            && host.equals(new URL(repo.getUrl()).getHost());
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

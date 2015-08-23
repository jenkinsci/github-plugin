package com.cloudbees.jenkins;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.jenkinsci.plugins.github.util.misc.NullSafeFunction;
import org.kohsuke.github.GHCommitPointer;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.notNull;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.withHost;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * Uniquely identifies a repository on GitHub.
 *
 * @author Kohsuke Kawaguchi
 */
public class GitHubRepositoryName {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubRepositoryName.class);

    private static final Pattern[] URL_PATTERNS = {
            /**
             * The first set of patterns extract the host, owner and repository names
             * from URLs that include a '.git' suffix, removing the suffix from the
             * repository name.
             */
            Pattern.compile("git@(.+):([^/]+)/([^/]+)\\.git"),
            Pattern.compile("https?://[^/]+@([^/]+)/([^/]+)/([^/]+)\\.git"),
            Pattern.compile("https?://([^/]+)/([^/]+)/([^/]+)\\.git"),
            Pattern.compile("git://([^/]+)/([^/]+)/([^/]+)\\.git"),
            Pattern.compile("ssh://git@([^/]+)/([^/]+)/([^/]+)\\.git"),
            /**
             * The second set of patterns extract the host, owner and repository names
             * from all other URLs. Note that these patterns must be processed *after*
             * the first set, to avoid any '.git' suffix that may be present being included
             * in the repository name.
             */
            Pattern.compile("git@(.+):([^/]+)/([^/]+)/?"),
            Pattern.compile("https?://[^/]+@([^/]+)/([^/]+)/([^/]+)/?"),
            Pattern.compile("https?://([^/]+)/([^/]+)/([^/]+)/?"),
            Pattern.compile("git://([^/]+)/([^/]+)/([^/]+)/?"),
            Pattern.compile("ssh://git@([^/]+)/([^/]+)/([^/]+)/?")
    };

    /**
     * Create {@link GitHubRepositoryName} from URL
     *
     * @param url must be non-null
     *
     * @return parsed {@link GitHubRepositoryName} or null if it cannot be
     * parsed from the specified URL
     */
    @CheckForNull
    public static GitHubRepositoryName create(@Nonnull final String url) {
        LOGGER.debug("Constructing from URL {}", url);
        for (Pattern p : URL_PATTERNS) {
            Matcher m = p.matcher(trimToEmpty(url));
            if (m.matches()) {
                LOGGER.debug("URL matches {}", m);
                GitHubRepositoryName ret = new GitHubRepositoryName(m.group(1), m.group(2), m.group(3));
                LOGGER.debug("Object is {}", ret);
                return ret;
            }
        }
        LOGGER.warn("Could not match URL {}", url);
        return null;
    }

    @SuppressWarnings("visibilitymodifier")
    public final String host;
    @SuppressWarnings("visibilitymodifier")
    public final String userName;
    @SuppressWarnings("visibilitymodifier")
    public final String repositoryName;

    public GitHubRepositoryName(String host, String userName, String repositoryName) {
        this.host = host;
        this.userName = userName;
        this.repositoryName = repositoryName;
    }

    public String getHost() {
        return host;
    }

    public String getUserName() {
        return userName;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    /**
     * Resolves this name to the actual reference by {@link GHRepository}
     *
     * Shortcut for {@link #resolve(Predicate)} with always true predicate
     * ({@link Predicates#alwaysTrue()}) as argument
     */
    public Iterable<GHRepository> resolve() {
        return resolve(Predicates.<GitHubServerConfig>alwaysTrue());
    }

    /**
     * Resolves this name to the actual reference by {@link GHRepository}.
     *
     * Since the system can store multiple credentials,
     * and only some of them might be able to see this name in question,
     * this method uses {@link org.jenkinsci.plugins.github.config.GitHubPluginConfig#findGithubConfig(Predicate)}
     * and attempt to find the right credential that can
     * access this repository.
     *
     * Any predicate as argument will be combined with {@link GitHubServerConfig#withHost(String)} to find only
     * corresponding for this repo name authenticated github repository
     *
     * This method walks multiple repositories for each credential that can access the repository. Depending on
     * what you are trying to do with the repository, you might have to keep trying until a {@link GHRepository}
     * with suitable permission is returned.
     *
     * @param predicate helps to filter only useful for resolve {@link GitHubServerConfig}s
     *
     * @return iterable with lazy login process for getting authenticated repos
     * @since 0.13.0
     */
    public Iterable<GHRepository> resolve(Predicate<GitHubServerConfig> predicate) {
        return from(GitHubPlugin.configuration().findGithubConfig(and(withHost(host), predicate)))
                .transform(toGHRepository(this))
                .filter(notNull());
    }

    /**
     * Variation of {@link #resolve()} method that just returns the first valid repository object.
     *
     * This is useful if the caller only relies on the read access to the repository and doesn't need to
     * walk possible candidates.
     */
    @CheckForNull
    public GHRepository resolveOne() {
        return from(resolve()).first().orNull();
    }

    /**
     * Does this repository match the repository referenced in the given {@link GHCommitPointer}?
     */
    public boolean matches(GHCommitPointer commit) {
        return userName.equals(commit.getUser().getLogin())
                && repositoryName.equals(commit.getRepository().getName())
                && host.equals(commit.getRepository().getHtmlUrl().getHost());
    }

    /**
     * Does this repository match the repository referenced in the given {@link GHCommitPointer}?
     */
    public boolean matches(GHRepository repo) throws IOException {
        return userName.equals(repo.getOwner().getLogin()) // TODO: use getOwnerName
                && repositoryName.equals(repo.getName())
                && host.equals(repo.getHtmlUrl().getHost());
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(host).append(userName).append(repositoryName).build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE)
                .append("host", host).append("username", userName).append("repository", repositoryName).build();
    }

    private static Function<GitHub, GHRepository> toGHRepository(final GitHubRepositoryName repoName) {
        return new NullSafeFunction<GitHub, GHRepository>() {
            @Override
            protected GHRepository applyNullSafe(@Nonnull GitHub gitHub) {
                try {
                    return gitHub.getRepository(format("%s/%s", repoName.getUserName(), repoName.getRepositoryName()));
                } catch (IOException e) {
                    LOGGER.warn("Failed to obtain repository {}", this, e);
                    return null;
                }
            }
        };
    }
}

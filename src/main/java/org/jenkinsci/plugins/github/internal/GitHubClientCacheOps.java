package org.jenkinsci.plugins.github.internal;

import com.cloudbees.jenkins.GitHubWebHook;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.hash.Hashing;
import com.squareup.okhttp.Cache;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.jenkinsci.plugins.github.util.misc.NullSafeFunction;
import org.jenkinsci.plugins.github.util.misc.NullSafePredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Files.notExists;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * Class with util functions to operate GitHub client cache
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.14.0
 */
public final class GitHubClientCacheOps {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubClientCacheOps.class);

    private GitHubClientCacheOps() {
    }

    /**
     * @return predicate which returns true if cache enabled for applied {@link GitHubServerConfig}
     */
    public static Predicate<GitHubServerConfig> withEnabledCache() {
        return new WithEnabledCache();
    }

    /**
     * @return function to convert {@link GitHubServerConfig} to {@link Cache}
     */
    public static Function<GitHubServerConfig, Cache> toCacheDir() {
        return new ToCacheDir();
    }

    /**
     * Extracts relative to base cache dir name of cache folder for each config
     * For example if the full path to cache folder is
     * "$JENKINS_HOME/org.jenkinsci.plugins.github.GitHubPlugin.cache/keirurna", this function returns "keirurna"
     *
     * @return function to extract folder name from cache object
     */
    public static Function<Cache, String> cacheToName() {
        return new CacheToName();
    }

    /**
     * To accept for cleaning only not active cache dirs
     *
     * @param caches set of active cache names, extracted with help of {@link #cacheToName()}
     *
     * @return filter to accept only names not in set
     */
    public static DirectoryStream.Filter<Path> notInCaches(Set<String> caches) {
        checkNotNull(caches, "set of active caches can't be null");
        return new NotInCachesFilter(caches);
    }

    /**
     * This directory contains all other cache dirs for each client config
     *
     * @return path to base cache directory.
     */
    public static Path getBaseCacheDir() {
        return new File(GitHubWebHook.getJenkinsInstance().getRootDir(),
                GitHubPlugin.class.getName() + ".cache").toPath();
    }

    /**
     * Removes all not active dirs with old caches.
     * This method is invoked after each save of global plugin config
     *
     * @param configs active server configs to exclude caches from cleanup
     */
    public static void clearRedundantCaches(List<GitHubServerConfig> configs) {
        Path baseCacheDir = getBaseCacheDir();

        if (notExists(baseCacheDir)) {
            return;
        }

        final Set<String> actualNames = from(configs).filter(withEnabledCache()).transform(toCacheDir())
                .transform(cacheToName()).toSet();

        try (DirectoryStream<Path> caches = newDirectoryStream(baseCacheDir, notInCaches(actualNames))) {
            deleteEveryIn(caches);
        } catch (IOException e) {
            LOGGER.warn("Can't list cache dirs in {}", baseCacheDir, e);
        }
    }

    /**
     * Removes directories with caches
     *
     * @param caches paths to directories to be removed
     */
    private static void deleteEveryIn(DirectoryStream<Path> caches) {
        for (Path notActualCache : caches) {
            LOGGER.debug("Deleting redundant cache dir {}", notActualCache);
            try {
                FileUtils.deleteDirectory(notActualCache.toFile());
            } catch (IOException e) {
                LOGGER.error("Can't delete cache dir <{}>", notActualCache, e);
            }
        }
    }

    /**
     * @see #withEnabledCache()
     */
    private static class WithEnabledCache extends NullSafePredicate<GitHubServerConfig> {
        @Override
        protected boolean applyNullSafe(@Nonnull GitHubServerConfig config) {
            return config.getClientCacheSize() > 0;
        }
    }

    /**
     * @see #toCacheDir()
     */
    private static class ToCacheDir extends NullSafeFunction<GitHubServerConfig, Cache> {

        public static final int MB = 1024 * 1024;

        @Override
        protected Cache applyNullSafe(@Nonnull GitHubServerConfig config) {
            checkArgument(config.getClientCacheSize() > 0, "Cache can't be with size <= 0");

            Path cacheDir = getBaseCacheDir().resolve(hashed(config));
            return new Cache(cacheDir.toFile(), (long) config.getClientCacheSize() * MB);
        }

        /**
         * @param config url and creds id to be hashed
         *
         * @return unique id for folder name to create cache inside of base cache dir
         */
        private static String hashed(GitHubServerConfig config) {
            return Hashing.murmur3_32().newHasher()
                    .putString(trimToEmpty(config.getApiUrl()))
                    .putString(trimToEmpty(config.getCredentialsId())).hash().toString();
        }
    }

    /**
     * @see #cacheToName()
     */
    private static class CacheToName extends NullSafeFunction<Cache, String> {
        @Override
        protected String applyNullSafe(@Nonnull Cache cache) {
            return cache.getDirectory().getName();
        }
    }

    /**
     * @see #notInCaches(Set)
     */
    private static class NotInCachesFilter implements DirectoryStream.Filter<Path> {
        private final Set<String> activeCacheNames;

        public NotInCachesFilter(Set<String> activeCacheNames) {
            this.activeCacheNames = activeCacheNames;
        }

        @Override
        public boolean accept(Path entry) {
            if (!isDirectory(entry)) {
                LOGGER.debug("{} is not a directory", entry);
                return false;
            }
            LOGGER.trace("Trying to find <{}> in active caches list...", entry);
            return !activeCacheNames.contains(String.valueOf(entry.getFileName()));
        }
    }
}

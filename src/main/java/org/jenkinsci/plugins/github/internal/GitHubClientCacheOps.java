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
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Files.notExists;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * @author lanwen (Merkushev Kirill)
 */
public final class GitHubClientCacheOps {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubClientCacheOps.class);

    private GitHubClientCacheOps() {
    }

    public static Predicate<GitHubServerConfig> withEnabledCache() {
        return new WithEnabledCache();
    }

    public static Function<GitHubServerConfig, Cache> toCacheDir() {
        return new ToCacheDir();
    }

    public static Function<Cache, String> cacheToName() {
        return new CacheToName();
    }

    public static DirectoryStream.Filter<Path> notInCaches(Set<String> caches) {
        checkNotNull(caches, "set of active caches can't be null");
        return new NotInCachesFilter(caches);
    }

    public static Path getBaseCacheDir() {
        return new File(GitHubWebHook.getJenkinsInstance().getRootDir(),
                GitHubPlugin.class.getName() + ".cache").toPath();
    }

    /**
     * Clean cache dirs for not cached configs
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

    private static class WithEnabledCache extends NullSafePredicate<GitHubServerConfig> {
        @Override
        protected boolean applyNullSafe(@Nonnull GitHubServerConfig config) {
            return config.getClientCacheSize() > 0;
        }

    }

    private static class ToCacheDir extends NullSafeFunction<GitHubServerConfig, Cache> {

        @Override
        protected Cache applyNullSafe(@Nonnull GitHubServerConfig config) {
            checkArgument(config.getClientCacheSize() > 0, "Cache can't be with size <= 0");

            Path cacheDir = getBaseCacheDir().resolve(hashed(config));

            return new Cache(cacheDir.toFile(), 1024 * 1024 * config.getClientCacheSize());
        }

        /**
         * @param config url and creds id to be hashed
         *
         * @return unique id for folder name to create cache inside of base cache dir
         */
        private String hashed(GitHubServerConfig config) {
            return Hashing.goodFastHash(32).newHasher()
                    .putString(trimToEmpty(config.getApiUrl()))
                    .putString(trimToEmpty(config.getCredentialsId())).hash().toString();
        }
    }

    private static class CacheToName extends NullSafeFunction<Cache, String> {
        @Override
        protected String applyNullSafe(@Nonnull Cache cache) {
            return cache.getDirectory().getName();
        }
    }

    private static class NotInCachesFilter implements DirectoryStream.Filter<Path> {
        private final Set<String> activeCacheNames;

        public NotInCachesFilter(Set<String> activeCacheNames) {
            this.activeCacheNames = activeCacheNames;
        }

        @Override
        public boolean accept(Path entry) {
            LOGGER.trace("Trying to find <{}> in active caches list...", entry);
            return !activeCacheNames.contains(String.valueOf(entry.getFileName()));
        }
    }
}

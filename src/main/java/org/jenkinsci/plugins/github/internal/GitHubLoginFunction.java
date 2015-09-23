package org.jenkinsci.plugins.github.internal;

import com.cloudbees.jenkins.GitHubWebHook;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.jenkinsci.plugins.github.util.misc.NullSafeFunction;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.RateLimitHandler;
import org.kohsuke.github.extras.OkHttpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.GITHUB_URL;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.tokenFor;

/**
 * Converts server config to authorized GH instance on {@link #applyNullSafe(GitHubServerConfig)}.
 * If login process is not successful it returns null
 *
 * Uses okHttp (https://github.com/square/okhttp) as connector to have ability to use cache and proxy
 * The capacity of cache can be changed with help of {@link GitHubLoginFunction#CACHE_SIZE_MB_FOR_GITHUB} as
 * system property.
 *
 * Don't use this class in any place directly
 * as of it have public static factory {@link GitHubServerConfig#loginToGithub()}
 *
 * @author lanwen (Merkushev Kirill)
 * @see GitHubServerConfig#loginToGithub()
 */
@Restricted(NoExternalUse.class)
public class GitHubLoginFunction extends NullSafeFunction<GitHubServerConfig, GitHub> {

    /**
     * Capacity of cache for GitHub client in MB.
     *
     * Can be overridden as system property -Dorg.jenkinsci.plugins.github.GitHubPlugin.cache.size.mb=1 on jenkins jvm
     * Set to <= 0 to turn off this feature
     *
     * Defaults to 20 MB
     * @since TODO
     */
    public static final int CACHE_SIZE_MB_FOR_GITHUB = Integer.getInteger(
            GitHubPlugin.class.getName() + ".cache.size.mb", 20
    );

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubLoginFunction.class);

    /**
     * Called by {@link #apply(Object)}
     * Logins to GH and returns client instance
     *
     * @param github config where token saved
     *
     * @return authorized client or null on login error
     */
    @Override
    @CheckForNull
    protected GitHub applyNullSafe(@Nonnull GitHubServerConfig github) {
        String accessToken = tokenFor(github.getCredentialsId());

        GitHubBuilder builder = new GitHubBuilder()
                .withOAuthToken(accessToken)
                .withConnector(connector(defaultIfBlank(github.getApiUrl(), GITHUB_URL)))
                .withRateLimitHandler(RateLimitHandler.FAIL);
        try {
            if (isNotBlank(github.getApiUrl())) {
                builder.withEndpoint(github.getApiUrl());
            }

            return builder.build();
        } catch (IOException e) {
            LOGGER.warn("Failed to login with creds {}", github.getCredentialsId(), e);
            return null;
        }
    }

    /**
     * Uses proxy if configured on pluginManager/advanced page
     *
     * @param apiUrl GitHub's url to build proxy to
     *
     * @return proxy to use it in connector
     */
    private Proxy getProxy(String apiUrl) {
        Jenkins jenkins = GitHubWebHook.getJenkinsInstance();

        if (jenkins.proxy == null) {
            return Proxy.NO_PROXY;
        } else {
            return jenkins.proxy.createProxy(apiUrl);
        }
    }

    /**
     * okHttp connector to be used as backend for GitHub client.
     * Uses proxy of jenkins
     * If cache size > 0, uses cache
     *
     * @param apiUrl to build proxy
     *
     * @return connector to be used as backend for client
     */
    private OkHttpConnector connector(String apiUrl) {
        Jenkins jenkins = GitHubWebHook.getJenkinsInstance();
        OkHttpClient client = new OkHttpClient().setProxy(getProxy(apiUrl));

        if (CACHE_SIZE_MB_FOR_GITHUB > 0) {
            File cacheDir = new File(jenkins.getRootDir(), GitHubPlugin.class.getName() + ".cache");
            Cache cache = new Cache(cacheDir, CACHE_SIZE_MB_FOR_GITHUB * 1024 * 1024);
            client.setCache(cache);
        }

        return new OkHttpConnector(new OkUrlFactory(client));
    }
}

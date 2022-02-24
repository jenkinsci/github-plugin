package org.jenkinsci.plugins.github.internal;

import com.cloudbees.jenkins.GitHubWebHook;
import io.jenkins.plugins.okhttp.api.JenkinsOkHttpClient;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.jenkinsci.plugins.github.util.misc.NullSafeFunction;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.RateLimitHandler;
import org.kohsuke.github.extras.okhttp3.OkHttpConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.GITHUB_URL;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.tokenFor;
import static org.jenkinsci.plugins.github.internal.GitHubClientCacheOps.toCacheDir;

/**
 * Converts server config to authorized GH instance on {@link #applyNullSafe(GitHubServerConfig)}.
 * If login process is not successful it returns null
 *
 * Uses okHttp (https://github.com/square/okhttp) as connector to have ability to use cache and proxy
 * The capacity of cache can be changed in advanced section of global configuration for plugin
 *
 * Don't use this class in any place directly
 * as of it have public static factory {@link GitHubServerConfig#loginToGithub()}
 *
 * @author lanwen (Merkushev Kirill)
 * @see GitHubServerConfig#loginToGithub()
 */
@Restricted(NoExternalUse.class)
public class GitHubLoginFunction extends NullSafeFunction<GitHubServerConfig, GitHub> {

    private static final OkHttpClient BASECLIENT = JenkinsOkHttpClient.newClientBuilder(new OkHttpClient()).build();
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
                .withConnector(connector(github))
                .withRateLimitHandler(RateLimitHandler.FAIL);
        try {
            if (isNotBlank(github.getApiUrl())) {
                builder.withEndpoint(github.getApiUrl());
            }
            LOGGER.debug("Create new GH client with creds id {}", github.getCredentialsId());
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
     * @return proxy to use it in connector. Should not be null as it can lead to unexpected behaviour
     */
    @Nonnull
    private Proxy getProxy(String apiUrl) {
        Jenkins jenkins = GitHubWebHook.getJenkinsInstance();

        if (jenkins.proxy == null) {
            return Proxy.NO_PROXY;
        } else {
            try {
                return jenkins.proxy.createProxy(new URL(apiUrl).getHost());
            } catch (MalformedURLException e) {
                return jenkins.proxy.createProxy(apiUrl);
            }
        }
    }

    /**
     * okHttp connector to be used as backend for GitHub client.
     * Uses proxy of jenkins
     * If cache size > 0, uses cache
     *
     * @return connector to be used as backend for client
     */
    private OkHttpConnector connector(GitHubServerConfig config) {
        OkHttpClient.Builder builder = BASECLIENT.newBuilder()
            .proxy(getProxy(defaultIfBlank(config.getApiUrl(), GITHUB_URL)));


        if (config.getClientCacheSize() > 0) {
            Cache cache = toCacheDir().apply(config);
            builder.cache(cache);
        }

        return new OkHttpConnector(builder.build());
    }
}

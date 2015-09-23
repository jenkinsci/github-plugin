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
import org.kohsuke.github.HttpConnector;
import org.kohsuke.github.RateLimitHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.jenkinsci.plugins.github.GitHubPlugin.configuration;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.GITHUB_URL;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.tokenFor;

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

        if (configuration().getClientCacheSize() > 0) {
            File cacheDir = new File(jenkins.getRootDir(), GitHubPlugin.class.getName() + ".cache");
            Cache cache = new Cache(cacheDir, configuration().getClientCacheSize() * 1024 * 1024);
            client.setCache(cache);
        }

        return new OkHttpConnector(new OkUrlFactory(client));
    }

    /**
     * Copy-paste due to class loading issues
     *
     * @see org.kohsuke.github.extras.OkHttpConnector
     */
    private static class OkHttpConnector implements HttpConnector {
        private final OkUrlFactory urlFactory;

        private OkHttpConnector(OkUrlFactory urlFactory) {
            this.urlFactory = urlFactory;
        }

        @Override
        public HttpURLConnection connect(URL url) throws IOException {
            return urlFactory.open(url);
        }
    }
}

package org.jenkinsci.plugins.github.config;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.internal.GitHubLoginFunction;
import org.jenkinsci.plugins.github.util.misc.NullSafeFunction;
import org.jenkinsci.plugins.github.util.misc.NullSafePredicate;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.firstOrDefault;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * This object represents configuration of each credentials-github pair.
 * If no api url explicitly defined, default url used.
 * So one github server can be used with many creds and one token can be used multiply times in lot of gh servers
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.13.0
 */
@XStreamAlias("github-server-config")
public class GitHubServerConfig extends AbstractDescribableImpl<GitHubServerConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubServerConfig.class);

    /**
     * Because of {@link GitHub} hide this const from external use we need to store it here
     */
    public static final String GITHUB_URL = "https://api.github.com";

    /**
     * Used as default token value if no any creds found by given credsId.
     */
    private static final String UNKNOWN_TOKEN = "UNKNOWN_TOKEN";

    /**
     * Default value in MB for client cache size
     *
     * @see #getClientCacheSize()
     */
    public static final int DEFAULT_CLIENT_CACHE_SIZE_MB = 20;

    private String apiUrl = GITHUB_URL;
    private boolean manageHooks = true;
    private final String credentialsId;

    /**
     * @see #getClientCacheSize()
     * @see #setClientCacheSize(int)
     */
    private int clientCacheSize = DEFAULT_CLIENT_CACHE_SIZE_MB;

    /**
     * only to set to default apiUrl when uncheck. Can be removed if optional block can nullify value if unchecked
     */
    private transient boolean customApiUrl;

    /**
     * To avoid creation of new one on every login with this config
     */
    private transient GitHub cachedClient;

    @DataBoundConstructor
    public GitHubServerConfig(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    /**
     * {@link #customApiUrl} field should be defined earlier. Because of we get full content of optional block,
     * even if it already unchecked. So if we want to return api url to default value - custom value should affect
     *
     * @param apiUrl custom url if GH. Default value will be used in case of custom is unchecked or value is blank
     */
    @DataBoundSetter
    public void setApiUrl(String apiUrl) {
        if (customApiUrl) {
            this.apiUrl = defaultIfBlank(apiUrl, GITHUB_URL);
        } else {
            this.apiUrl = GITHUB_URL;
        }
    }

    /**
     * Should be called before {@link #setApiUrl(String)}
     *
     * @param customApiUrl true if optional block "Custom GH Api Url" checked in UI
     */
    @DataBoundSetter
    public void setCustomApiUrl(boolean customApiUrl) {
        this.customApiUrl = customApiUrl;
    }

    /**
     * This server config will be used to manage GH Hooks if true
     *
     * @param manageHooks false to ignore this config on hook auto-management
     */
    @DataBoundSetter
    public void setManageHooks(boolean manageHooks) {
        this.manageHooks = manageHooks;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    /**
     * @see #isUrlCustom(String)
     */
    @Restricted(NoExternalUse.class)
    public boolean isCustomApiUrl() {
        return isUrlCustom(apiUrl);
    }

    public boolean isManageHooks() {
        return manageHooks;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Capacity of cache for GitHub client in MB.
     *
     * Defaults to 20 MB
     *
     * @since TODO
     */
    public int getClientCacheSize() {
        return clientCacheSize;
    }

    /**
     * @param clientCacheSize capacity of cache for GitHub client in MB, set to <= 0 to turn off this feature
     */
    @DataBoundSetter
    public void setClientCacheSize(int clientCacheSize) {
        this.clientCacheSize = clientCacheSize;
    }

    /**
     * @return cached GH client or null
     */
    private GitHub getCachedClient() {
        return cachedClient;
    }

    /**
     * Used by {@link org.jenkinsci.plugins.github.config.GitHubServerConfig.ClientCacheFunction}
     *
     * @param cachedClient updated client. Maybe null to invalidate cache
     */
    private synchronized void setCachedClient(GitHub cachedClient) {
        this.cachedClient = cachedClient;
    }

    /**
     * Checks GH url for equality to default api url
     *
     * @param apiUrl should be not blank and not equal to default url to return true
     *
     * @return true if url not blank and not equal to default
     */
    public static boolean isUrlCustom(String apiUrl) {
        return isNotBlank(apiUrl) && !GITHUB_URL.equals(apiUrl);
    }

    /**
     * Converts server config to authorized GH instance. If login process is not successful it returns null
     *
     * @return function to convert config to gh instance
     * @see org.jenkinsci.plugins.github.config.GitHubServerConfig.ClientCacheFunction
     */
    @CheckForNull
    public static Function<GitHubServerConfig, GitHub> loginToGithub() {
        return new ClientCacheFunction();
    }

    /**
     * Tries to find {@link StringCredentials} by id and returns token from it.
     * Returns {@link #UNKNOWN_TOKEN} if no any creds found with this id.
     *
     * @param credentialsId id to find creds
     *
     * @return token from creds or default non empty string
     */
    @Nonnull
    public static String tokenFor(String credentialsId) {
        StringCredentialsImpl unkn = new StringCredentialsImpl(null, null, null, Secret.fromString(UNKNOWN_TOKEN));
        return firstOrDefault(
                lookupCredentials(StringCredentials.class,
                        Jenkins.getInstance(), ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()),
                withId(credentialsId), unkn).getSecret().getPlainText();
    }

    /**
     * Returns true if given host is part of stored (or default if blank) api url
     *
     * For example:
     * withHost(api.github.com).apply(config for ~empty~) = true
     * withHost(api.github.com).apply(config for api.github.com) = true
     * withHost(api.github.com).apply(config for github.company.com) = false
     *
     * @param host host to find in api url
     *
     * @return predicate to match against {@link GitHubServerConfig}
     */
    public static Predicate<GitHubServerConfig> withHost(final String host) {
        return new NullSafePredicate<GitHubServerConfig>() {
            @Override
            protected boolean applyNullSafe(@Nonnull GitHubServerConfig github) {
                return defaultIfEmpty(github.getApiUrl(), GITHUB_URL).contains(host);
            }
        };
    }

    /**
     * Returns true if config can be used in hooks managing
     *
     * @return predicate to match against {@link GitHubServerConfig}
     */
    public static Predicate<GitHubServerConfig> allowedToManageHooks() {
        return new NullSafePredicate<GitHubServerConfig>() {
            @Override
            protected boolean applyNullSafe(@NonNull GitHubServerConfig github) {
                return github.isManageHooks();
            }
        };
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GitHubServerConfig> {

        @Override
        public String getDisplayName() {
            return "GitHub Server Config";
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems(@QueryParameter String apiUrl) {
            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return new ListBoxModel();
            }
            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withAll(lookupCredentials(
                                    StringCredentials.class,
                                    Jenkins.getInstance(),
                                    ACL.SYSTEM, fromUri(defaultIfBlank(apiUrl, GITHUB_URL)).build())
                    );
        }

        @SuppressWarnings("unused")
        public FormValidation doVerifyCredentials(
                @QueryParameter String apiUrl,
                @QueryParameter String credentialsId,
                @QueryParameter Integer clientCacheSize) throws IOException {

            GitHubServerConfig config = new GitHubServerConfig(credentialsId);
            config.setCustomApiUrl(isUrlCustom(apiUrl));
            config.setApiUrl(apiUrl);
            config.setClientCacheSize(clientCacheSize);
            GitHub gitHub = new GitHubLoginFunction().apply(config);

            try {
                if (gitHub != null && gitHub.isCredentialValid()) {
                    return FormValidation.ok("Credentials verified for user %s, rate limit: %s",
                            gitHub.getMyself().getLogin(), gitHub.getRateLimit().remaining);
                } else {
                    return FormValidation.error("Failed to validate the account");
                }
            } catch (IOException e) {
                return FormValidation.error(e, "Failed to validate the account");
            }
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckApiUrl(@QueryParameter String value) {
            try {
                new URL(value);
            } catch (MalformedURLException e) {
                return FormValidation.error("Malformed GitHub url (%s)", e.getMessage());
            }

            if (GITHUB_URL.equals(value)) {
                return FormValidation.ok();
            }

            if (value.endsWith("/api/v3") || value.endsWith("/api/v3/")) {
                return FormValidation.ok();
            }

            return FormValidation.warning("GitHub Enterprise API URL ends with \"/api/v3\"");
        }
    }

    /**
     * Function to get authorized GH client and cache it in config
     * has {@link #loginToGithub()} static factory
     */
    private static class ClientCacheFunction extends NullSafeFunction<GitHubServerConfig, GitHub> {
        @Override
        protected GitHub applyNullSafe(@Nonnull GitHubServerConfig github) {
            if (github.getCachedClient() == null) {
                github.setCachedClient(new GitHubLoginFunction().apply(github));
            }
            return github.getCachedClient();
        }
    }
}

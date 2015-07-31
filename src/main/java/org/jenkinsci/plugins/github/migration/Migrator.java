package org.jenkinsci.plugins.github.migration;

import com.cloudbees.jenkins.GitHubPushTrigger;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.jenkinsci.plugins.github.config.GitHubTokenCredentialsCreator;
import org.jenkinsci.plugins.github.deprecated.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.isUrlCustom;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * Helper class incapsulates migration process from old configs to new ones
 * After 1.12.0 this plugin uses {@link GitHubPlugin} to store all global configuration instead of
 * push trigger descriptor
 *
 * @author lanwen (Merkushev Kirill)
 * @since TODO
 */
public class Migrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Migrator.class);

    /**
     * Loads {@link GitHubPushTrigger.DescriptorImpl} and migrate all values
     * to {@link org.jenkinsci.plugins.github.config.GitHubPluginConfig}
     *
     * @throws IOException if any read-save problems as it critical to work process of this plugin
     */
    public void migrate() throws IOException {
        LOGGER.debug("Check if GitHub Plugin needs config migration");
        GitHubPushTrigger.DescriptorImpl descriptor = GitHubPushTrigger.DescriptorImpl.get();
        descriptor.load();

        if (isNotEmpty(descriptor.getCredentials())) {
            LOGGER.warn("Migration for old GitHub Plugin credentials started");
            GitHubPlugin.configuration().getConfigs().addAll(
                    from(descriptor.getCredentials()).transform(toGHServerConfig()).toList()
            );

            descriptor.clearCredentials();
            descriptor.save();
            GitHubPlugin.get().save();
        }

        if (descriptor.getDeprecatedHookUrl() != null) {
            LOGGER.warn("Migration for old GitHub Plugin hook url started");
            GitHubPlugin.configuration().setOverrideHookUrl(true);
            GitHubPlugin.configuration().setHookUrl(descriptor.getDeprecatedHookUrl());
            descriptor.clearDeprecatedHookUrl();
            descriptor.save();
            GitHubPlugin.get().save();
        }
    }

    /**
     * Creates new string credentials from token
     *
     * @return converter to get all useful info from old plain creds and crete new server config
     */
    @VisibleForTesting
    protected Function<Credential, GitHubServerConfig> toGHServerConfig() {
        return new Function<Credential, GitHubServerConfig>() {
            @Override
            public GitHubServerConfig apply(Credential input) {
                LOGGER.info("Migrate GitHub Plugin creds for {} {}", input.getUsername(), input.getApiUrl());
                GitHubTokenCredentialsCreator creator =
                        Jenkins.getInstance().getDescriptorByType(GitHubTokenCredentialsCreator.class);

                StandardCredentials credentials = creator.createCredentials(
                        input.getApiUrl(),
                        input.getOauthAccessToken(),
                        input.getUsername()
                );

                GitHubServerConfig gitHubServerConfig = new GitHubServerConfig(credentials.getId());
                gitHubServerConfig.setCustom(isUrlCustom(input.getApiUrl()));
                gitHubServerConfig.setApiUrl(input.getApiUrl());

                return gitHubServerConfig;
            }
        };
    }

    /**
     * - Old plain credentials moved to deprecated package as used only for migration
     */
    public static void enableCompatibilityAliases() {
        Jenkins.XSTREAM2.addCompatibilityAlias("com.cloudbees.jenkins.Credential", Credential.class);
    }
}

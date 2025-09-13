package org.jenkinsci.plugins.github.test;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.config.HookSecretConfig;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * Helper class for setting the secret text for hooks while testing.
 */
public class HookSecretHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(HookSecretHelper.class);

    private HookSecretHelper() {
    }

    /**
     * Stores the secret and sets it as the current hook secret.
     * 
     * @param config where to save
     * @param secretText The secret/key.
     */
    public static void storeSecretIn(GitHubPluginConfig config, final String secretText) {
        final StringCredentialsImpl credentials = createCredentail(secretText);

        config.setHookSecretConfigs(Collections.singletonList(new HookSecretConfig(credentials.getId())));
    }

    private static @NotNull StringCredentialsImpl createCredentail(String secretText) {
        final StringCredentialsImpl credentials = new StringCredentialsImpl(
                CredentialsScope.GLOBAL,
                UUID.randomUUID().toString(),
                null,
                Secret.fromString(secretText)
        );

        ACL.impersonate(ACL.SYSTEM, new Runnable() {
            @Override
            public void run() {
                try {
                    new SystemCredentialsProvider.StoreImpl().addCredentials(
                            Domain.global(),
                            credentials
                    );

                } catch (IOException e) {
                    LOGGER.error("Unable to set hook secret", e);
                }
            }
        });
        return credentials;
    }

    /**
     * Stores the secret and sets it as the current hook secret.
     * @param secretText The secret/key.
     */
    public static void storeSecret(final String secretText) {
        storeSecretIn(Jenkins.getInstance().getDescriptorByType(GitHubPluginConfig.class), secretText);
    }

    /**
     * Stores the secret and sets it as the current hook secret.
     */
    public static void storeGitHubPluginConfigWithNullSecret(GitHubPluginConfig config) {
        config.setHookSecretConfigs(Collections.singletonList(new HookSecretConfig(null)));
    }

    /**
     * Unsets the current hook secret.
     *
     * @param config where to remove
     */
    public static void removeSecretIn(GitHubPluginConfig config) {
        config.setHookSecretConfigs(null);
    }

    /**
     * Unsets the current hook secret.
     */
    public static void removeSecret() {
        removeSecretIn(Jenkins.getInstance().getDescriptorByType(GitHubPluginConfig.class));
    }
}

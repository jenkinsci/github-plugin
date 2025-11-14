package org.jenkinsci.plugins.github.config;

import org.jenkinsci.plugins.github.GitHubPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.jenkinsci.plugins.github.test.HookSecretHelper.storeSecret;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Test for storing hook secrets.
 */
@WithJenkins
@SuppressWarnings("deprecation")
class HookSecretConfigTest {

    private static final String SECRET_INIT = "test";

    private JenkinsRule jenkinsRule;

    private HookSecretConfig hookSecretConfig;

    @BeforeEach
    void setup(JenkinsRule rule) {
        jenkinsRule = rule;
        storeSecret(SECRET_INIT);
    }

    @Test
    void shouldStoreNewSecrets() {
        storeSecret(SECRET_INIT);

        hookSecretConfig = GitHubPlugin.configuration().getHookSecretConfig();
        assertNotNull(hookSecretConfig.getHookSecret(), "Secret is persistent");
        assertEquals(SECRET_INIT, hookSecretConfig.getHookSecret().getPlainText(), "Secret correctly stored");
    }

    @Test
    void shouldOverwriteExistingSecrets() {
        final String newSecret = "test2";
        storeSecret(newSecret);

        hookSecretConfig = GitHubPlugin.configuration().getHookSecretConfig();
        assertNotNull(hookSecretConfig.getHookSecret(), "Secret is persistent");
        assertEquals(newSecret, hookSecretConfig.getHookSecret().getPlainText(), "Secret correctly stored");
    }
}
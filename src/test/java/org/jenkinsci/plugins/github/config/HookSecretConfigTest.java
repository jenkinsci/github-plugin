package org.jenkinsci.plugins.github.config;

import org.jenkinsci.plugins.github.GitHubPlugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.jenkinsci.plugins.github.test.HookSecretHelper.storeSecret;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * Test for storing hook secrets.
 */
public class HookSecretConfigTest {

    private static final String SECRET_INIT = "test";

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private HookSecretConfig hookSecretConfig;

    @Before
    public void setup() {
        storeSecret(SECRET_INIT);
        hookSecretConfig = GitHubPlugin.configuration().getHookSecretConfig();
    }

    @Test
    public void shouldStoreNewSecrets() {
        storeSecret(SECRET_INIT);

        assertNotNull("Secret is persistent", hookSecretConfig.getHookSecret());
        assertTrue("Secret correctly stored", SECRET_INIT.equals(hookSecretConfig.getHookSecret().getPlainText()));
    }

    @Test
    public void shouldOverwriteExistingSecrets() {
        final String newSecret = "test2";
        storeSecret(newSecret);

        assertNotNull("Secret is persistent", hookSecretConfig.getHookSecret());
        assertTrue("Secret correctly stored", newSecret.equals(hookSecretConfig.getHookSecret().getPlainText()));
    }
}
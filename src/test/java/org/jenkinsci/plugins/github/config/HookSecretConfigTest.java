package org.jenkinsci.plugins.github.config;

import jenkins.model.Jenkins;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * Test for storing hook secrets.
 */
public class HookSecretConfigTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private HookSecretConfig hookSecretConfig;

    @Before
    public void setup() {
        hookSecretConfig = Jenkins.getInstance().getDescriptorByType(HookSecretConfig.class);
    }

    @Test
    public void shouldStoreNewSecrets() {
        final String secret = "test";
        hookSecretConfig.storeSecret(secret);

        assertNotNull("Secret is persistent", hookSecretConfig.getHookSecret());
        assertTrue("Secret correctly stored", secret.equals(hookSecretConfig.getHookSecret().getPlainText()));
    }

    @Test
    public void shouldOverwriteExistingSecrets() {
        final String secret = "test";
        final String newSecret = "test2";
        hookSecretConfig.storeSecret(secret);
        hookSecretConfig.storeSecret(newSecret);

        assertNotNull("Secret is persistent", hookSecretConfig.getHookSecret());
        assertTrue("Secret correctly stored", newSecret.equals(hookSecretConfig.getHookSecret().getPlainText()));
    }
}
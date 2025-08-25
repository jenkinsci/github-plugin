package org.jenkinsci.plugins.github.config;

import org.jenkinsci.plugins.github.webhook.SignatureAlgorithm;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for SHA-256 configuration in {@link HookSecretConfig}.
 * 
 * @since 1.45.0
 */
public class HookSecretConfigSHA256Test {

    @Test
    public void shouldDefaultToSHA256Algorithm() {
        HookSecretConfig config = new HookSecretConfig("test-credentials");
        
        assertThat("Should default to SHA-256 algorithm", 
                  config.getSignatureAlgorithm(), equalTo(SignatureAlgorithm.SHA256));
    }

    @Test
    public void shouldAcceptExplicitSHA256Algorithm() {
        HookSecretConfig config = new HookSecretConfig("test-credentials", SignatureAlgorithm.SHA256);
        
        assertThat("Should use explicitly set SHA-256 algorithm", 
                  config.getSignatureAlgorithm(), equalTo(SignatureAlgorithm.SHA256));
    }

    @Test
    public void shouldAcceptSHA1Algorithm() {
        HookSecretConfig config = new HookSecretConfig("test-credentials", SignatureAlgorithm.SHA1);
        
        assertThat("Should use explicitly set SHA-1 algorithm", 
                  config.getSignatureAlgorithm(), equalTo(SignatureAlgorithm.SHA1));
    }

    @Test
    public void shouldDefaultToSHA256WhenNullAlgorithmProvided() {
        HookSecretConfig config = new HookSecretConfig("test-credentials", null);
        
        assertThat("Should default to SHA-256 when null algorithm provided", 
                  config.getSignatureAlgorithm(), equalTo(SignatureAlgorithm.SHA256));
    }
}
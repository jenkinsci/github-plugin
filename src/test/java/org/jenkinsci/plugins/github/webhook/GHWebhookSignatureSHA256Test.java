package org.jenkinsci.plugins.github.webhook;

import hudson.util.Secret;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for SHA-256 functionality in {@link GHWebhookSignature}.
 * 
 * @since 1.45.0
 */
public class GHWebhookSignatureSHA256Test {

    private static final String SECRET_CONTENT = "It's a Secret to Everybody";
    private static final String PAYLOAD = "Hello, World!";
    // Expected SHA-256 signature based on GitHub's documentation
    private static final String EXPECTED_SHA256_DIGEST = "757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17";

    @Test
    public void shouldComputeCorrectSHA256Signature() {
        Secret secret = Secret.fromString(SECRET_CONTENT);
        GHWebhookSignature signature = GHWebhookSignature.webhookSignature(PAYLOAD, secret);
        
        String computed = signature.sha256();
        
        assertThat("SHA-256 signature should match expected value", 
                  computed, equalTo(EXPECTED_SHA256_DIGEST));
    }

    @Test
    public void shouldValidateSHA256SignatureCorrectly() {
        Secret secret = Secret.fromString(SECRET_CONTENT);
        GHWebhookSignature signature = GHWebhookSignature.webhookSignature(PAYLOAD, secret);
        
        boolean isValid = signature.matches(EXPECTED_SHA256_DIGEST, SignatureAlgorithm.SHA256);
        
        assertThat("Valid SHA-256 signature should be accepted", isValid, equalTo(true));
    }

    @Test
    public void shouldRejectInvalidSHA256Signature() {
        Secret secret = Secret.fromString(SECRET_CONTENT);
        GHWebhookSignature signature = GHWebhookSignature.webhookSignature(PAYLOAD, secret);
        
        String invalidDigest = "invalid_signature_digest";
        boolean isValid = signature.matches(invalidDigest, SignatureAlgorithm.SHA256);
        
        assertThat("Invalid SHA-256 signature should be rejected", isValid, equalTo(false));
    }

    @Test
    public void shouldRejectSHA1SignatureWhenExpectingSHA256() {
        String secretContent = "test-secret";
        Secret secret = Secret.fromString(secretContent);
        GHWebhookSignature signature = GHWebhookSignature.webhookSignature(PAYLOAD, secret);
        
        // Get SHA-1 digest but try to validate as SHA-256
        String sha1Digest = signature.sha1();
        boolean isValid = signature.matches(sha1Digest, SignatureAlgorithm.SHA256);
        
        assertThat("SHA-1 signature should be rejected when expecting SHA-256", 
                  isValid, equalTo(false));
    }

    @Test
    public void shouldHandleDifferentPayloads() {
        Secret secret = Secret.fromString(SECRET_CONTENT);
        String payload1 = "payload1";
        String payload2 = "payload2";
        
        GHWebhookSignature signature1 = GHWebhookSignature.webhookSignature(payload1, secret);
        GHWebhookSignature signature2 = GHWebhookSignature.webhookSignature(payload2, secret);
        
        String digest1 = signature1.sha256();
        String digest2 = signature2.sha256();
        
        assertThat("Different payloads should produce different signatures", 
                  digest1.equals(digest2), equalTo(false));
                  
        // Each signature should validate its own payload
        assertThat("Signature 1 should validate payload 1", 
                  signature1.matches(digest1, SignatureAlgorithm.SHA256), equalTo(true));
        assertThat("Signature 2 should validate payload 2", 
                  signature2.matches(digest2, SignatureAlgorithm.SHA256), equalTo(true));
                  
        // Cross-validation should fail
        assertThat("Signature 1 should not validate payload 2's digest", 
                  signature1.matches(digest2, SignatureAlgorithm.SHA256), equalTo(false));
    }
}
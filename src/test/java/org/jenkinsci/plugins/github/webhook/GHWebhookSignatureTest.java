package org.jenkinsci.plugins.github.webhook;

import hudson.util.Secret;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.jenkinsci.plugins.github.webhook.GHWebhookSignature.webhookSignature;
import static org.junit.Assert.assertThat;

/**
 * Tests for utility class that deals with crypto/hashing of data.
 *
 * @author martinmine
 */
public class GHWebhookSignatureTest {

    private static final String SIGNATURE = "85d155c55ed286a300bd1cf124de08d87e914f3a";
    private static final String PAYLOAD = "foo";
    private static final String SECRET = "bar";

    // Taken from real example of Pull Request update webhook payload
    private static final String UNICODE_PAYLOAD = "{\"description\":\"foo\\u00e2\\u0084\\u00a2\"}";
    private static final String UNICODE_SIGNATURE = "10e3cb05d27049775aeca89d84d9e6123d5ab006";

    @ClassRule
    public static JenkinsRule jRule = new JenkinsRule();

    @Test
    public void shouldComputeSHA1Signature() throws Exception {
        assertThat("signature is valid", webhookSignature(
                PAYLOAD,
                Secret.fromString(SECRET)
        ).sha1(), equalTo(SIGNATURE));
    }

    @Test
    public void shouldMatchSignature() throws Exception {
        assertThat("signature should match", webhookSignature(
                PAYLOAD,
                Secret.fromString(SECRET)
        ).matches(SIGNATURE), equalTo(true));
    }

    @Test
    public void shouldComputeSHA1SignatureWithUnicodePayload() throws Exception {
        assertThat("signature is valid for unicode payload", webhookSignature(
                UNICODE_PAYLOAD,
                Secret.fromString(SECRET)
        ).sha1(), equalTo(UNICODE_SIGNATURE));
    }
}

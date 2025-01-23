package org.jenkinsci.plugins.github.extension;

import hudson.util.Secret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.jenkinsci.plugins.github.webhook.GHWebhookSignature.webhookSignature;

/**
 * Tests for utility class that deals with crypto/hashing of data.
 *
 * @author martinmine
 */
@WithJenkins
class CryptoUtilTest {

    private static final String SIGNATURE = "85d155c55ed286a300bd1cf124de08d87e914f3a";
    private static final String PAYLOAD = "foo";
    private static final String SECRET = "bar";

    private JenkinsRule jRule;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jRule = rule;
    }

    @Test
    void shouldComputeSHA1Signature() throws Exception {
        assertThat("signature is valid", webhookSignature(
                PAYLOAD,
                Secret.fromString(SECRET)
        ).sha1(), equalTo(SIGNATURE));
    }

    @Test
    void shouldMatchSignature() throws Exception {
        assertThat("signature should match", webhookSignature(
                PAYLOAD,
                Secret.fromString(SECRET)
        ).matches(SIGNATURE), equalTo(true));
    }
}
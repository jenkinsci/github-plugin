package org.jenkinsci.plugins.github.extension;

import hudson.util.Secret;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.jenkinsci.plugins.github.extension.CryptoUtil.selectSecret;
import static org.junit.Assert.*;

/**
 * Tests for utility class that deals with crypto/hashing of data.
 * @author martinmine
 */
@RunWith(MockitoJUnitRunner.class)
public class CryptoUtilTest {

    private static final String SIGNATURE = "85d155c55ed286a300bd1cf124de08d87e914f3a";
    private static final String PAYLOAD = "foo";
    private Secret globalSecret;
    private Secret projectSecret;
    private Secret secret;

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setupSecrets() {
        globalSecret = Secret.fromString("global secret");
        projectSecret = Secret.fromString("project secret");
        secret = Secret.fromString("bar");
    }

    @Test
    public void shouldUseProjectSecretOverGlobalSecret() throws Exception {
        final Secret selected = selectSecret(globalSecret, projectSecret);

        assertThat("secret is project", selected, equalTo(projectSecret));
    }

    @Test
    public void shouldUseProjectSecretWhenGlobalSecretIsNotPresent() throws Exception {
        final Secret selected = selectSecret(null, projectSecret);

        assertThat("secret is project", selected, equalTo(projectSecret));
    }

    @Test
    public void shouldUseGlobalSecretWhenProjectSecretIsNotPresent() throws Exception {
        final Secret selected = selectSecret(globalSecret, null);

        assertThat("secret is global", selected, equalTo(globalSecret));
    }

    @Test
    public void shouldReturnNullWhenNoSecretsArePresent() throws Exception {
        final Secret selected = selectSecret(null, null);

        assertThat("secret is null", selected, nullValue());
    }

    @Test
    public void shouldComputeSHA1Signature() throws Exception {
        final String signature = CryptoUtil.computeSHA1Signature(PAYLOAD, secret);

        assertThat("signature is valid", signature, equalTo(SIGNATURE));
    }

    @Test
    public void shouldParseCorrectSHA1Signature() throws Exception {
        final String parsedSignature = CryptoUtil.parseSHA1Value("sha1=" + SIGNATURE);
        assertThat("parsed signature is valid", parsedSignature, equalTo(SIGNATURE));
    }

    @Test
    public void shouldReturnNullWithNoSignature() throws Exception {
        final String parsedSignature = CryptoUtil.parseSHA1Value(null);
        assertThat("signature is null", parsedSignature, nullValue());
    }

    @Test
    public void shouldReturnSecret() throws Exception {
        final Secret secret = CryptoUtil.generateSecret();
        assertThat("secret is not null", secret, notNullValue());
    }

    @Test
    public void shouldReturnUniqueSecret() throws Exception {
        final Secret firstSecret = CryptoUtil.generateSecret();
        final Secret secondSecret = CryptoUtil.generateSecret();

        assertNotSame("secrets are different", secondSecret.getPlainText(), firstSecret.getPlainText());
    }
}
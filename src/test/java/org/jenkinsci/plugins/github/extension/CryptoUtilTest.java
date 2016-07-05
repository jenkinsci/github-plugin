package org.jenkinsci.plugins.github.extension;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

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

    private static final String GLOBAL_SECRET = "global secret";
    private static final String PROJECT_SECRET = "project secret";
    private static final String EXPECTED_SIGNATURE = "85d155c55ed286a300bd1cf124de08d87e914f3a";
    private static final String PAYLOAD = "foo";
    private static final String SECRET = "bar";

    @Test
    public void shouldUseProjectSecretOverGlobalSecret() throws Exception {
        final String selected = selectSecret(GLOBAL_SECRET, PROJECT_SECRET);

        assertThat("secret is project", selected, equalTo(PROJECT_SECRET));
    }

    @Test
    public void shouldUseProjectSecretWhenGlobalSecretIsNotPresent() throws Exception {
        final String selected = selectSecret(null, PROJECT_SECRET);

        assertThat("secret is project", selected, equalTo(PROJECT_SECRET));
    }

    @Test
    public void shouldUseGlobalSecretWhenProjectSecretIsNotPresent() throws Exception {
        final String selected = selectSecret(GLOBAL_SECRET, null);

        assertThat("secret is global", selected, equalTo(GLOBAL_SECRET));
    }

    @Test
    public void shouldReturnNullWhenNoSecretsArePresent() throws Exception {
        final String selected = selectSecret(null, null);

        assertThat("secret is null", selected, nullValue());
    }

    @Test
    public void computeSHA1Signature() throws Exception {
        final String signature = CryptoUtil.computeSHA1Signature(PAYLOAD, SECRET);

        assertThat("signature is valid", signature, equalTo(EXPECTED_SIGNATURE));
    }
}
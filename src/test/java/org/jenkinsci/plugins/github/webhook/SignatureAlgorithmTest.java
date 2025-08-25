package org.jenkinsci.plugins.github.webhook;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for {@link SignatureAlgorithm}.
 * 
 * @since 1.45.0
 */
public class SignatureAlgorithmTest {

    @Test
    public void shouldHaveCorrectSHA256Properties() {
        SignatureAlgorithm algorithm = SignatureAlgorithm.SHA256;
        
        assertThat("SHA-256 prefix", algorithm.getPrefix(), equalTo("sha256"));
        assertThat("SHA-256 header", algorithm.getHeaderName(), equalTo("X-Hub-Signature-256"));
        assertThat("SHA-256 Java algorithm", algorithm.getJavaAlgorithm(), equalTo("HmacSHA256"));
        assertThat("SHA-256 signature prefix", algorithm.getSignaturePrefix(), equalTo("sha256="));
    }

    @Test
    public void shouldHaveCorrectSHA1Properties() {
        SignatureAlgorithm algorithm = SignatureAlgorithm.SHA1;
        
        assertThat("SHA-1 prefix", algorithm.getPrefix(), equalTo("sha1"));
        assertThat("SHA-1 header", algorithm.getHeaderName(), equalTo("X-Hub-Signature"));
        assertThat("SHA-1 Java algorithm", algorithm.getJavaAlgorithm(), equalTo("HmacSHA1"));
        assertThat("SHA-1 signature prefix", algorithm.getSignaturePrefix(), equalTo("sha1="));
    }

    @Test
    public void shouldDefaultToSHA256() {
        assertThat("Default algorithm should be SHA-256", 
                  SignatureAlgorithm.DEFAULT, equalTo(SignatureAlgorithm.SHA256));
    }
}
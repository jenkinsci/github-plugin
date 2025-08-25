package org.jenkinsci.plugins.github.webhook;

import hudson.util.Secret;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.security.MessageDigest;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class for dealing with signatures of incoming requests.
 *
 * @see <a href=https://developer.github.com/webhooks/#payloads>API documentation</a>
 * @since 1.21.0
 */
public class GHWebhookSignature {

    private static final Logger LOGGER = LoggerFactory.getLogger(GHWebhookSignature.class);
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    public static final String INVALID_SIGNATURE = "COMPUTED_INVALID_SIGNATURE";

    private final String payload;
    private final Secret secret;

    private GHWebhookSignature(String payload, Secret secret) {
        this.payload = payload;
        this.secret = secret;
    }

    /**
     * @param payload Clear-text to create signature of.
     * @param secret  Key to sign with.
     */
    public static GHWebhookSignature webhookSignature(String payload, Secret secret) {
        checkNotNull(payload, "Payload can't be null");
        checkNotNull(secret, "Secret should be defined to compute sign");
        return new GHWebhookSignature(payload, secret);
    }


    /**
     * Computes a RFC 2104-compliant HMAC digest using SHA1 of a payloadFrom with a given key (secret).
     *
     * @deprecated Use {@link #sha256()} for enhanced security
     * @return HMAC digest of payloadFrom using secret as key. Will return COMPUTED_INVALID_SIGNATURE
     * on any exception during computation.
     */
    @Deprecated
    public String sha1() {
        return computeSignature(HMAC_SHA1_ALGORITHM);
    }

    /**
     * Computes a RFC 2104-compliant HMAC digest using SHA256 of a payload with a given key (secret).
     * This is the recommended method for webhook signature validation.
     *
     * @return HMAC digest of payload using secret as key. Will return COMPUTED_INVALID_SIGNATURE
     * on any exception during computation.
     * @since 1.45.0
     */
    public String sha256() {
        return computeSignature(HMAC_SHA256_ALGORITHM);
    }
    
    /**
     * Computes HMAC signature using the specified algorithm.
     *
     * @param algorithm The HMAC algorithm to use (e.g., "HmacSHA1", "HmacSHA256")
     * @return HMAC digest as hex string, or INVALID_SIGNATURE on error
     */
    private String computeSignature(String algorithm) {
        try {
            final SecretKeySpec keySpec = new SecretKeySpec(secret.getPlainText().getBytes(UTF_8), algorithm);
            final Mac mac = Mac.getInstance(algorithm);
            mac.init(keySpec);
            final byte[] rawHMACBytes = mac.doFinal(payload.getBytes(UTF_8));

            return Hex.encodeHexString(rawHMACBytes);
        } catch (Exception e) {
            LOGGER.error("Error computing {} signature", algorithm, e);
            return INVALID_SIGNATURE;
        }
    }

    /**
     * @param digest computed signature from external place (GitHub)
     *
     * @return true if computed and provided signatures identical
     * @deprecated Use {@link #matches(String, SignatureAlgorithm)} for explicit algorithm selection
     */
    @Deprecated
    public boolean matches(String digest) {
        return matches(digest, SignatureAlgorithm.SHA1);
    }
    
    /**
     * Validates a signature using the specified algorithm.
     * Uses constant-time comparison to prevent timing attacks.
     *
     * @param digest the signature to validate (without algorithm prefix)
     * @param algorithm the signature algorithm to use
     * @return true if computed and provided signatures match
     * @since 1.45.0
     */
    public boolean matches(String digest, SignatureAlgorithm algorithm) {
        String computed;
        switch (algorithm) {
            case SHA256:
                computed = sha256();
                break;
            case SHA1:
                computed = sha1();
                break;
            default:
                LOGGER.warn("Unsupported signature algorithm: {}", algorithm);
                return false;
        }
        
        LOGGER.trace("Signature validation: algorithm={} calculated={} provided={}", 
                    algorithm, computed, digest);
        
        if (digest == null && computed == null) {
            return true;
        } else if (digest == null || computed == null) {
            return false;
        } else {
            // Use constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(computed.getBytes(UTF_8), digest.getBytes(UTF_8));
        }
    }
}

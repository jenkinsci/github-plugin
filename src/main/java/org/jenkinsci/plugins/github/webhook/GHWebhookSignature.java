package org.jenkinsci.plugins.github.webhook;

import hudson.util.Secret;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
     * @return HMAC digest of payloadFrom using secret as key. Will return COMPUTED_INVALID_SIGNATURE
     * on any exception during computation.
     */
    public String sha1() {
        try {
            final SecretKeySpec keySpec = new SecretKeySpec(secret.getPlainText().getBytes(UTF_8), HMAC_SHA1_ALGORITHM);
            final Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(keySpec);
            final byte[] rawHMACBytes = mac.doFinal(payload.getBytes(UTF_8));

            return Hex.encodeHexString(rawHMACBytes);
        } catch (Exception e) {
            LOGGER.error("", e);
            return INVALID_SIGNATURE;
        }
    }

    /**
     * @param digest computed signature from external place (GitHub)
     *
     * @return true if computed and provided signatures identical
     */
    public boolean matches(String digest) {
        String computed = sha1();
        LOGGER.trace("Signature: calculated={} provided={}", computed, digest);
        return StringUtils.equals(computed, digest);
    }
}

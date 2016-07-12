package org.jenkinsci.plugins.github.extension;

import hudson.util.Secret;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility class for dealing with signatures of incoming requests.
 * @see <a href=https://developer.github.com/webhooks/#payloads>API documentation</a>
 */
public class CryptoUtil {
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoUtil.class);
    public static final String INVALID_SIGNATURE = "INVALID_SIGNATURE";

    private CryptoUtil() {
    }

    /**
     * Computes a RFC 2104-compliant HMAC digest using SHA1 of a payload with a given key (secret).
     * @param payload Clear-text to create signature of.
     * @param secret Key to sign with.
     * @return HMAC digest of payload using secret as key. Will return INVALID_SIGNATURE if any args is null.
     */
    @Nullable
    public static String computeSHA1Signature(@Nullable final String payload, @Nullable final Secret secret) {
        if (payload == null || secret == null) {
            return INVALID_SIGNATURE;
        }

        try {
            final SecretKeySpec keySpec = new SecretKeySpec(secret.getPlainText().getBytes(), HMAC_SHA1_ALGORITHM);
            final Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(keySpec);
            final byte[] rawHMACBytes = mac.doFinal(payload.getBytes("UTF-8"));

            return Hex.encodeHexString(rawHMACBytes);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Grabs the value after "sha1=" in a string.
     * @param digest The string to get the sha1 value from.
     * @return Value after "sha1" present in the digest value. Null if not present.
     */
    @Nullable
    public static String parseSHA1Value(@Nullable final String digest) {
        if (digest != null && digest.startsWith("sha1=")) {
            return digest.substring(5);
        } else {
            return null;
        }
    }
}

package org.jenkinsci.plugins.github.extension;

import hudson.util.Secret;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Utility class for dealing with signatures of incoming requests.
 * @see <a href=https://developer.github.com/webhooks/#payloads>API documentation</a>
 */
public class CryptoUtil {
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final Logger LOGGER = LoggerFactory.getLogger(CryptoUtil.class);
    private static final SecureRandom random = new SecureRandom();

    /**
     * Selects the appropriate secret to be used. Project-specific secrets override globally configured secrets.
     * If not secret is configured, return null.
     * @param globalSecret Globally used secret among all hooks.
     * @param projectSecret Secret specific to one hook.
     * @return Secret to use. Null if no secrets are configured.
     */
    public static @Nullable Secret selectSecret(@Nullable Secret globalSecret, @Nullable Secret projectSecret) {
        if (projectSecret != null) {
            return projectSecret;
        } else if (globalSecret != null) {
            return globalSecret;
        } else {
            return null;
        }
    }

    /**
     * Computes a RFC 2104-compliant HMAC digest using SHA1 of a payload with a given key (secret).
     * @param payload Clear-text to create signature of.
     * @param secret Key to sign with.
     * @return HMAC digest of payload using secret as key.
     */
    public static @Nullable String computeSHA1Signature(@Nonnull final String payload, final @Nonnull Secret secret) {
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
    public static @Nullable String parseSHA1Value(@Nullable final String digest) {
        if (digest != null && digest.startsWith("sha1=")) {
            return digest.substring(5);
        } else {
            return null;
        }
    }

    /**
     * Generates a random secret being used as shared secret between the Jenkins CI and GitHub.
     * @return A 60 character long random string.
     */
    public static Secret generateSecret() {
        return Secret.fromString(new BigInteger(130, random).toString(60));
    }
}

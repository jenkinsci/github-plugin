package org.jenkinsci.plugins.github.webhook;

/**
 * Enumeration of supported webhook signature algorithms.
 *
 * @since 1.45.0
 */
public enum SignatureAlgorithm {
    /**
     * SHA-256 HMAC signature validation (recommended).
     * Uses X-Hub-Signature-256 header with sha256= prefix.
     */
    SHA256("sha256", "X-Hub-Signature-256", "HmacSHA256"),

    /**
     * SHA-1 HMAC signature validation (legacy).
     * Uses X-Hub-Signature header with sha1= prefix.
     *
     * @deprecated Use SHA256 for enhanced security
     */
    @Deprecated
    SHA1("sha1", "X-Hub-Signature", "HmacSHA1");

    private final String prefix;
    private final String headerName;
    private final String javaAlgorithm;

    /**
     * System property to override default signature algorithm.
     * Set to "SHA1" to use legacy SHA-1 as default for backwards compatibility.
     */
    public static final String DEFAULT_ALGORITHM_PROPERTY = "jenkins.github.webhook.signature.default";

    /**
     * Gets the default algorithm for new configurations.
     * Defaults to SHA-256 for security, but can be overridden via system property.
     * This is evaluated dynamically to respect system property changes.
     *
     * @return the default algorithm based on current system property
     */
    public static SignatureAlgorithm getDefault() {
        return getDefaultAlgorithm();
    }

    SignatureAlgorithm(String prefix, String headerName, String javaAlgorithm) {
        this.prefix = prefix;
        this.headerName = headerName;
        this.javaAlgorithm = javaAlgorithm;
    }

    /**
     * @return the prefix used in signature strings (e.g. "sha256", "sha1")
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return the HTTP header name for this algorithm
     */
    public String getHeaderName() {
        return headerName;
    }

    /**
     * @return the Java algorithm name for HMAC computation
     */
    public String getJavaAlgorithm() {
        return javaAlgorithm;
    }

    /**
     * @return the expected signature prefix including equals sign (e.g. "sha256=", "sha1=")
     */
    public String getSignaturePrefix() {
        return prefix + "=";
    }

    /**
     * Determines the default signature algorithm based on system property.
     * Defaults to SHA-256 for security, but allows SHA-1 override for legacy environments.
     *
     * @return the default algorithm to use
     */
    private static SignatureAlgorithm getDefaultAlgorithm() {
        String property = System.getProperty(DEFAULT_ALGORITHM_PROPERTY);
        if (property == null || property.trim().isEmpty()) {
            // No property set, use secure SHA-256 default
            return SHA256;
        }
        try {
            return SignatureAlgorithm.valueOf(property.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Invalid property value, default to secure SHA-256
            return SHA256;
        }
    }
}

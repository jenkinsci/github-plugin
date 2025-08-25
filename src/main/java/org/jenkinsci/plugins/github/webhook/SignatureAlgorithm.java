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
     * Default algorithm for new configurations - SHA-256 for security.
     */
    public static final SignatureAlgorithm DEFAULT = SHA256;

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
}

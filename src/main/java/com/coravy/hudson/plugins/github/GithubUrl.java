package com.coravy.hudson.plugins.github;

import org.apache.commons.lang.StringUtils;

/**
 * @author Stefan Saasen <stefan@coravy.com>
 */
public final class GithubUrl {

    /**
     * Normalizes the github URL.
     * <p>
     * Removes unwanted path elements (e.g. <code>tree/master</code>).
     *
     * @return URL to the project or null if input is invalid.
     */
    private static String normalize(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        // Strip "/tree/..."
        if (url.contains("/tree/")) {
            url = url.replaceFirst("/tree/.*$", "");
        }
        if (!url.endsWith("/")) {
            url += '/';
        }
        return url;
    }

    private final String baseUrl;

    GithubUrl(final String input) {
        this.baseUrl = normalize(input);
    }

    @Override
    public String toString() {
        return this.baseUrl;
    }

    public String baseUrl() {
        return this.baseUrl;
    }

    /**
     * Returns the URL to a particular commit.
     *
     * @param id - the git SHA1 hash
     *
     * @return URL String (e.g. http://github.com/juretta/github-plugin/commit/5e31203faea681c41577b685818a361089fac1fc)
     */
    public String commitId(final String id) {
        return new StringBuilder().append(baseUrl).append("commit/").append(id).toString();
    }

}

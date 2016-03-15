package org.jenkinsci.plugins.github.util;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author lanwen (Merkushev Kirill)
 */
public final class XSSApi {
    private XSSApi() {
    }

    /**
     * Method to filter invalid url for XSS. This url can be inserted to href safely
     *
     * @param urlString unsafe url
     *
     * @return safe url
     */
    public static String asValidHref(String urlString) {
        try {
            return new URL(urlString).toExternalForm();
        } catch (MalformedURLException e) {
            return "";
        }
    }
}

package org.jenkinsci.plugins.github.util;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author lanwen (Merkushev Kirill)
 */
@Restricted(NoExternalUse.class)
public final class XSSApi {
    private static final Logger LOG = LoggerFactory.getLogger(XSSApi.class);

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
            LOG.debug("Malformed url - {}, empty string will be returned", urlString);
            return "";
        }
    }
}

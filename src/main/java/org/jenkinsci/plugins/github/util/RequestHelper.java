package org.jenkinsci.plugins.github.util;

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;

import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Helper class for reading body of HTTP requests.
 */
public class RequestHelper {
    private static final Logger LOGGER = getLogger(RequestHelper.class);

    private RequestHelper() {
    }

    public static String readRequestBody(final StaplerRequest req) {
        try {
            return IOUtils.toString(req.getInputStream(), Charsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        }
    }
}

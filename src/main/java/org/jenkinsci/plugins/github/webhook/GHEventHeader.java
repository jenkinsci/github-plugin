package org.jenkinsci.plugins.github.webhook;

import org.kohsuke.github.GHEvent;
import org.kohsuke.stapler.AnnotationHandler;
import org.kohsuke.stapler.InjectedParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.slf4j.Logger;

import jakarta.servlet.ServletException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.apache.commons.lang3.StringUtils.upperCase;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * InjectedParameter annotation to use on WebMethod parameters.
 * Handles GitHub's X-GitHub-Event header.
 *
 * @author lanwen (Merkushev Kirill)
 * @see <a href=https://wiki.jenkins-ci.org/display/JENKINS/Web+Method>Web Method</a>
 */
@Retention(RUNTIME)
@Target(PARAMETER)
@Documented
@InjectedParameter(GHEventHeader.PayloadHandler.class)
public @interface GHEventHeader {
    class PayloadHandler extends AnnotationHandler<GHEventHeader> {
        /**
         * @see <a href=https://developer.github.com/webhooks/#delivery-headers>Developer manual</a>
         */
        public static final String EVENT_HEADER = "X-GitHub-Event";
        private static final Logger LOGGER = getLogger(PayloadHandler.class);

        /**
         * @param type should be combined with type of {@link GHEvent}
         *
         * @return parsed {@link GHEvent} or null on empty header or unknown value
         */
        @Override
        public Object parse(StaplerRequest2 req, GHEventHeader a, Class type, String param) throws ServletException {
            isTrue(GHEvent.class.isAssignableFrom(type),
                    "Parameter '%s' should has type %s, not %s", param,
                    GHEvent.class.getName(),
                    type.getName()
            );

            String header = req.getHeader(EVENT_HEADER);
            LOGGER.debug("Header {} -> {}", EVENT_HEADER, header);

            if (header == null) {
                return null;
            }

            try {
                return GHEvent.valueOf(upperCase(header));
            } catch (IllegalArgumentException e) {
                LOGGER.debug("Unknown event - {}", e.getMessage());
                return null;
            }
        }
    }
}

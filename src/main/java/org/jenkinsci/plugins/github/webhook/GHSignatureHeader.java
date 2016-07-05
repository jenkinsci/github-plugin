package org.jenkinsci.plugins.github.webhook;

import org.kohsuke.stapler.AnnotationHandler;
import org.kohsuke.stapler.InjectedParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * InjectedParameter annotation to use on WebMethod parameters.
 * Grabs the X-Hub-Signature header value from HTTP requests and injects them to function parameters.
 *
 * @author lanwen (Merkushev Kirill)
 * @author martinmine
 * @see <a href=https://wiki.jenkins-ci.org/display/JENKINS/Web+Method>Web Method</a>
 */
@Retention(RUNTIME)
@Target(PARAMETER)
@Documented
@InjectedParameter(GHSignatureHeader.PayloadHandler.class)
public @interface GHSignatureHeader {
    class PayloadHandler extends AnnotationHandler<GHSignatureHeader> {
        private static final Logger LOGGER = getLogger(PayloadHandler.class);

        /**
         * Header key being used for the payload signatures.
         * @see <a href=https://developer.github.com/webhooks/>Developer manual</a>
         */
        public static final String SIGNATURE_HEADER = "X-Hub-Signature";

        /**
         * @return header value for payload signature. Null if header is not set.
         */
        @Override
        public Object parse(StaplerRequest req, GHSignatureHeader a, Class type, String param) throws ServletException {
            final String header = req.getHeader(SIGNATURE_HEADER);
            LOGGER.debug("Header {} -> {}", SIGNATURE_HEADER, header);

            return header;
        }
    }
}

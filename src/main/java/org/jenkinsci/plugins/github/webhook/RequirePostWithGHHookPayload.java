package org.jenkinsci.plugins.github.webhook;

import com.cloudbees.jenkins.GitHubWebHook;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.util.FluentIterableWrapper;
import org.kohsuke.github.GHEvent;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.Interceptor;
import org.kohsuke.stapler.interceptor.InterceptorAnnotation;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.security.interfaces.RSAPublicKey;

import static com.cloudbees.jenkins.GitHubWebHook.X_INSTANCE_IDENTITY;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static org.apache.commons.codec.binary.Base64.encodeBase64;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.jenkinsci.plugins.github.extension.CryptoUtil.INVALID_SIGNATURE;
import static org.jenkinsci.plugins.github.extension.CryptoUtil.computeSHA1Signature;
import static org.jenkinsci.plugins.github.extension.CryptoUtil.parseSHA1Value;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;
import static org.kohsuke.stapler.HttpResponses.error;
import static org.kohsuke.stapler.HttpResponses.errorWithoutStack;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * InterceptorAnnotation annotation to use on WebMethod signature.
 * Encapsulates preprocess logic of parsing GHHook or test connection request
 *
 * @author lanwen (Merkushev Kirill)
 * @see <a href=https://wiki.jenkins-ci.org/display/JENKINS/Web+Method>Web Method</a>
 */
@Retention(RUNTIME)
@Target({METHOD, FIELD})
@InterceptorAnnotation(RequirePostWithGHHookPayload.Processor.class)
public @interface RequirePostWithGHHookPayload {
    class Processor extends Interceptor {
        private static final Logger LOGGER = getLogger(Processor.class);
        /**
         * Header key being used for the payload signatures.
         * @see <a href=https://developer.github.com/webhooks/>Developer manual</a>
         */
        public static final String SIGNATURE_HEADER = "X-Hub-Signature";

        @Override
        public Object invoke(StaplerRequest req, StaplerResponse rsp, Object instance, Object[] arguments)
                throws IllegalAccessException, InvocationTargetException {

            shouldBePostMethod(req);
            returnsInstanceIdentityIfLocalUrlTest(req);
            shouldContainParseablePayload(arguments);
            shouldProvideValidSignature(req, arguments);

            return target.invoke(req, rsp, instance, arguments);
        }

        /**
         * Duplicates {@link @org.kohsuke.stapler.interceptor.RequirePOST} precheck.
         * As of it can't guarantee order of multiply interceptor calls,
         * it should implement all features of required interceptors in one class
         *
         * @throws InvocationTargetException if method os not POST
         */
        protected void shouldBePostMethod(StaplerRequest request) throws InvocationTargetException {
            if (!request.getMethod().equals("POST")) {
                throw new InvocationTargetException(error(SC_METHOD_NOT_ALLOWED, "Method POST required"));
            }
        }

        /**
         * Used for {@link GitHubPluginConfig#doCheckHookUrl(String)}}
         */
        protected void returnsInstanceIdentityIfLocalUrlTest(StaplerRequest req) throws InvocationTargetException {
            if (req.getHeader(GitHubWebHook.URL_VALIDATION_HEADER) != null) {
                // when the configuration page provides the self-check button, it makes a request with this header.
                throw new InvocationTargetException(new HttpResponses.HttpResponseException() {
                    @Override
                    public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node)
                            throws IOException, ServletException {
                        RSAPublicKey key = new InstanceIdentity().getPublic();
                        rsp.setStatus(HttpServletResponse.SC_OK);
                        rsp.setHeader(X_INSTANCE_IDENTITY, new String(encodeBase64(key.getEncoded()), UTF_8));
                    }
                });
            }
        }

        /**
         * Precheck arguments contains not null GHEvent and not blank payload.
         * If any other argument will be added to root action index method, then arg count check should be changed
         *
         * @param arguments event and payload. Both not null and not blank
         *
         * @throws InvocationTargetException if any of preconditions is not satisfied
         */
        protected void shouldContainParseablePayload(Object[] arguments) throws InvocationTargetException {
            isTrue(arguments.length == 2,
                    "GHHook root action should take <(GHEvent) event> and <(String) payload> only");

            FluentIterableWrapper<Object> from = from(newArrayList(arguments));

            isTrue(
                    from.firstMatch(instanceOf(GHEvent.class)).isPresent(),
                    "Hook should contain event type"
            );
            isTrue(
                    isNotBlank((String) from.firstMatch(instanceOf(String.class)).or("")),
                    "Hook should contain payload"
            );
        }

        /**
         * Checks that an incoming request has a valid signature, if there is specified a signature in the config.
         *
         * @param req Incoming request.
         * @throws InvocationTargetException if any of preconditions is not satisfied
         */
        protected void shouldProvideValidSignature(StaplerRequest req, Object[] args) throws InvocationTargetException {
            final String signature = parseSHA1Value(req.getHeader(SIGNATURE_HEADER));
            final Secret secret = Jenkins.getInstance()
                    .getDescriptorByType(GitHubPluginConfig.class).getHookSecretConfig().getHookSecret();
            final String payload = obtainRequestBody(req, args);
            final String computedSignature = computeSHA1Signature(payload, secret);

            if (secret != null) {
                isTrue(
                        signature != null && !INVALID_SIGNATURE.equals(signature),
                        "Signature must be specified in the header " + SIGNATURE_HEADER
                );

                isTrue(
                        computedSignature != null,
                        "Missing payload"
                );

                isTrue(
                        computedSignature.equals(signature),
                        String.format("Signatures did not match, computed signature was: %s", computedSignature)
                );
            }
        }

        protected String obtainRequestBody(StaplerRequest req, Object[] args) {
            final String parsedPayload = (String) args[1];

            if (req.getContentType().equals(GHEventPayload.PayloadHandler.APPLICATION_JSON)) {
                return parsedPayload;
            } else if (req.getContentType().equals(GHEventPayload.PayloadHandler.FORM_URLENCODED)) {
                try {
                    return String.format("payload=%s", URLEncoder.encode(parsedPayload, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            } else {
                LOGGER.error("Unknown content type {}", req.getContentType());
            }

            return null;
        }

        /**
         * Utility method to stop preprocessing if condition is false
         *
         * @param condition on false throws exception
         * @param msg       to add to exception
         *
         * @throws InvocationTargetException BAD REQUEST 400 status code with message
         */
        private void isTrue(boolean condition, String msg) throws InvocationTargetException {
            if (!condition) {
                throw new InvocationTargetException(errorWithoutStack(SC_BAD_REQUEST, msg));
            }
        }
    }
}


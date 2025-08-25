package org.jenkinsci.plugins.github.webhook;

import com.cloudbees.jenkins.GitHubWebHook;
import com.google.common.base.Optional;
import hudson.util.Secret;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.config.HookSecretConfig;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.util.FluentIterableWrapper;
import org.kohsuke.github.GHEvent;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.Interceptor;
import org.kohsuke.stapler.interceptor.InterceptorAnnotation;
import org.slf4j.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

import static com.cloudbees.jenkins.GitHubWebHook.X_INSTANCE_IDENTITY;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static org.apache.commons.codec.binary.Base64.encodeBase64;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
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
         * Header key being used for the legacy SHA-1 payload signatures.
         *
         * @see <a href=https://developer.github.com/webhooks/>Developer manual</a>
         * @deprecated Use SHA-256 signatures with X-Hub-Signature-256 header
         */
        @Deprecated
        public static final String SIGNATURE_HEADER = "X-Hub-Signature";
        
        /**
         * Header key being used for the SHA-256 payload signatures (recommended).
         *
         * @see <a href=https://docs.github.com/en/developers/webhooks-and-events/webhooks/securing-your-webhooks>GitHub Documentation</a>
         * @since 1.45.0
         */
        public static final String SIGNATURE_HEADER_SHA256 = "X-Hub-Signature-256";
        
        private static final String SHA1_PREFIX = "sha1=";
        private static final String SHA256_PREFIX = "sha256=";

        @Override
        public Object invoke(StaplerRequest2 req, StaplerResponse2 rsp, Object instance, Object[] arguments)
                throws IllegalAccessException, InvocationTargetException, ServletException {

            shouldBePostMethod(req);
            returnsInstanceIdentityIfLocalUrlTest(req);
            shouldContainParseablePayload(arguments);
            shouldProvideValidSignature(req, arguments);

            return target.invoke(req, rsp, instance, arguments);
        }

        /**
         * Duplicates {@link org.kohsuke.stapler.interceptor.RequirePOST} precheck.
         * As of it can't guarantee order of multiply interceptor calls,
         * it should implement all features of required interceptors in one class
         *
         * @throws InvocationTargetException if method os not POST
         */
        protected void shouldBePostMethod(StaplerRequest2 request) throws InvocationTargetException {
            if (!request.getMethod().equals("POST")) {
                throw new InvocationTargetException(error(SC_METHOD_NOT_ALLOWED, "Method POST required"));
            }
        }

        /**
         * Used for {@link GitHubPluginConfig#doCheckHookUrl(String)}}
         */
        protected void returnsInstanceIdentityIfLocalUrlTest(StaplerRequest2 req) throws InvocationTargetException {
            if (req.getHeader(GitHubWebHook.URL_VALIDATION_HEADER) != null) {
                // when the configuration page provides the self-check button, it makes a request with this header.
                throw new InvocationTargetException(new HttpResponses.HttpResponseException() {
                    @Override
                    public void generateResponse(StaplerRequest2 req, StaplerResponse2 rsp, Object node)
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
         * Checks that an incoming request has a valid signature,
         * if a hook secret is specified in the GitHub plugin config.
         * If no hook secret is configured, then the signature is ignored.
         * 
         * Uses the configured signature algorithm (SHA-256 by default, SHA-1 for legacy support).
         *
         * @param req Incoming request.
         * @throws InvocationTargetException if any of preconditions is not satisfied
         */
        protected void shouldProvideValidSignature(StaplerRequest2 req, Object[] args)
                throws InvocationTargetException {
            List<HookSecretConfig> secretConfigs = GitHubPlugin.configuration().getHookSecretConfigs();
            
            if (!secretConfigs.isEmpty()) {
                boolean validSignatureFound = false;
                
                for (HookSecretConfig config : secretConfigs) {
                    Secret secret = config.getHookSecret();
                    if (secret == null) {
                        continue;
                    }
                    
                    SignatureAlgorithm algorithm = config.getSignatureAlgorithm();
                    String headerName = algorithm.getHeaderName();
                    String expectedPrefix = algorithm.getSignaturePrefix();
                    
                    Optional<String> signHeader = Optional.fromNullable(req.getHeader(headerName));
                    if (!signHeader.isPresent()) {
                        LOGGER.debug("No signature header {} found for algorithm {}", headerName, algorithm);
                        continue;
                    }
                    
                    String fullSignature = signHeader.get();
                    if (!fullSignature.startsWith(expectedPrefix)) {
                        LOGGER.debug("Signature header {} does not start with expected prefix {}", 
                                   fullSignature, expectedPrefix);
                        continue;
                    }
                    
                    String digest = substringAfter(fullSignature, expectedPrefix);
                    LOGGER.trace("Verifying {} signature from header {}", algorithm, fullSignature);
                    
                    boolean isValid = GHWebhookSignature.webhookSignature(payloadFrom(req, args), secret)
                                                        .matches(digest, algorithm);
                    
                    if (isValid) {
                        validSignatureFound = true;
                        
                        // Log deprecation warning for SHA-1 usage
                        if (algorithm == SignatureAlgorithm.SHA1) {
                            LOGGER.warn("Using deprecated SHA-1 signature validation. " +
                                      "Consider upgrading webhook configuration to use SHA-256 for enhanced security.");
                        } else {
                            LOGGER.debug("Successfully validated {} signature", algorithm);
                        }
                        break;
                    } else {
                        LOGGER.debug("Signature validation failed for algorithm {}", algorithm);
                    }
                }
                
                isTrue(validSignatureFound, 
                       "No valid signature found. Ensure webhook is configured with a supported signature algorithm " +
                       "(SHA-256 recommended, SHA-1 for legacy compatibility).");
            }
        }

        /**
         * Extracts parsed payload from args and prepare it to calculating hash
         * (if json - pass as is, if form - url-encode it with prefix)
         *
         * @return ready-to-hash payload
         */
        protected String payloadFrom(StaplerRequest2 req, Object[] args) {
            final String parsedPayload = (String) args[1];

            if (req.getContentType().equals(GHEventPayload.PayloadHandler.APPLICATION_JSON)) {
                return parsedPayload;
            } else if (req.getContentType().equals(GHEventPayload.PayloadHandler.FORM_URLENCODED)) {
                try {
                    return String.format("payload=%s", URLEncoder.encode(
                            parsedPayload,
                            StandardCharsets.UTF_8.toString())
                    );
                } catch (UnsupportedEncodingException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            } else {
                LOGGER.error("Unknown content type {}", req.getContentType());

            }
            return "";
        }

        /**
         * Utility method to stop preprocessing if condition is false
         *
         * @param condition on false throws exception
         * @param msg       to add to exception
         * @throws InvocationTargetException BAD REQUEST 400 status code with message
         */
        private void isTrue(boolean condition, String msg) throws InvocationTargetException {
            if (!condition) {
                throw new InvocationTargetException(errorWithoutStack(SC_BAD_REQUEST, msg));
            }
        }
    }
}


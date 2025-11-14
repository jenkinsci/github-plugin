package org.jenkinsci.plugins.github.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.github.GHEvent;
import org.kohsuke.stapler.StaplerRequest2;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.jenkinsci.plugins.github.test.HookSecretHelper.removeSecret;
import static org.jenkinsci.plugins.github.test.HookSecretHelper.storeSecret;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@WithJenkins
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequirePostWithGHHookPayloadTest {

    private static final String SECRET_CONTENT = "secret";
    private static final String PAYLOAD = "sample payload";

    @Mock
    private StaplerRequest2 req;

    private JenkinsRule jenkinsRule;

    @Spy
    private RequirePostWithGHHookPayload.Processor processor;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkinsRule = rule;
        storeSecret(SECRET_CONTENT);
    }

    @Test
    void shouldPassOnlyPost() throws Exception {
        when(req.getMethod()).thenReturn("POST");
        new RequirePostWithGHHookPayload.Processor().shouldBePostMethod(req);
    }

    @Test
    void shouldNotPassOnNotPost() {
        when(req.getMethod()).thenReturn("GET");
        assertThrows(InvocationTargetException.class, () ->
                new RequirePostWithGHHookPayload.Processor().shouldBePostMethod(req));
    }

    @Test
    void shouldPassOnGHEventAndNotBlankPayload() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[]{GHEvent.PUSH, "{}"});
    }

    @Test
    void shouldNotPassOnNullGHEventAndNotBlankPayload() {
        assertThrows(InvocationTargetException.class, () ->
                new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                        new Object[]{null, "{}"}));
    }

    @Test
    void shouldNotPassOnGHEventAndBlankPayload() {
        assertThrows(InvocationTargetException.class, () ->
                new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                        new Object[]{GHEvent.PUSH, " "}));
    }

    @Test
    void shouldNotPassOnNulls() {
        assertThrows(InvocationTargetException.class, () ->
                new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                        new Object[]{null, null}));
    }

    @Test
    void shouldNotPassOnGreaterCountOfArgs() {
        assertThrows(InvocationTargetException.class, () ->
                new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                        new Object[]{GHEvent.PUSH, "{}", " "}
                ));
    }

    @Test
    void shouldNotPassOnLessCountOfArgs() {
        assertThrows(InvocationTargetException.class, () ->
                new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                        new Object[]{GHEvent.PUSH}
                ));
    }

    @Test
    @Issue("JENKINS-37481")
    void shouldPassOnAbsentSignatureInRequestIfSecretIsNotConfigured() throws Exception {
        removeSecret();

        processor.shouldProvideValidSignature(req, null);
    }

    @Test
    @Issue("JENKINS-48012")
    void shouldNotPassOnAbsentSignatureInRequest() {
        assertThrows(InvocationTargetException.class, () ->
                processor.shouldProvideValidSignature(req, null));
    }

    @Test
    void shouldNotPassOnInvalidSignature() {
        final String signature = "sha1=a94a8fe5ccb19ba61c4c0873d391e987982fbbd3";
        when(req.getHeader(RequirePostWithGHHookPayload.Processor.SIGNATURE_HEADER)).thenReturn(signature);
        doReturn(PAYLOAD).when(processor).payloadFrom(req, null);
        assertThrows(InvocationTargetException.class, () ->
                processor.shouldProvideValidSignature(req, null));
    }

    @Test
    void shouldNotPassOnMalformedSignature() {
        final String signature = "49d5f5cf800a81f257324912969a2d325d13d3fc";
        when(req.getHeader(RequirePostWithGHHookPayload.Processor.SIGNATURE_HEADER)).thenReturn(signature);
        doReturn(PAYLOAD).when(processor).payloadFrom(req, null);
        assertThrows(InvocationTargetException.class, () ->
                processor.shouldProvideValidSignature(req, null));
    }

    @Test
    void shouldPassWithValidSignature() throws Exception {
        final String signature = "sha1=49d5f5cf800a81f257324912969a2d325d13d3fc";
        final String signature256 = "sha256=569beaec8ea1c9deccec283d0bb96aeec0a77310c70875343737ae72cffa7044";

        when(req.getHeader(RequirePostWithGHHookPayload.Processor.SIGNATURE_HEADER)).thenReturn(signature);
        when(req.getHeader(RequirePostWithGHHookPayload.Processor.SIGNATURE_HEADER_SHA256)).thenReturn(signature256);
        doReturn(PAYLOAD).when(processor).payloadFrom(req, null);

        processor.shouldProvideValidSignature(req, null);
    }

    @Test
    @Issue("JENKINS-37481")
    void shouldIgnoreSignHeaderOnNotDefinedSignInConfig() throws Exception {
        removeSecret();
        final String signature = "sha1=49d5f5cf800a81f257324912969a2d325d13d3fc";

        when(req.getHeader(RequirePostWithGHHookPayload.Processor.SIGNATURE_HEADER)).thenReturn(signature);

        processor.shouldProvideValidSignature(req, null);
    }

    @Test
    void shouldReturnValidPayloadOnApplicationJson() {
        final String payload = "test";

        doReturn(GHEventPayload.PayloadHandler.APPLICATION_JSON).when(req).getContentType();

        final String body = processor.payloadFrom(req, new Object[]{null, payload});

        assertThat("valid returned body", body, equalTo(payload));
    }

    @Test
    void shouldReturnValidPayloadOnFormUrlEncoded() {
        final String payload = "test";

        doReturn(GHEventPayload.PayloadHandler.FORM_URLENCODED).when(req).getContentType();

        final String body = processor.payloadFrom(req, new Object[]{null, payload});

        assertThat("valid returned body", body, equalTo("payload=" + payload));
    }
}

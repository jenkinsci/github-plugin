package org.jenkinsci.plugins.github.webhook;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GHEvent;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.jenkinsci.plugins.github.test.HookSecretHelper.storeSecret;
import static org.jenkinsci.plugins.github.test.HookSecretHelper.removeSecret;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class RequirePostWithGHHookPayloadTest {

    private static final String SECRET_CONTENT = "secret";
    private static final String PAYLOAD = "sample payload";

    @Mock
    private StaplerRequest req;

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Spy
    private RequirePostWithGHHookPayload.Processor processor;

    @Before
    public void setSecret() {
        storeSecret(SECRET_CONTENT);
    }

    @Test
    public void shouldPassOnlyPost() throws Exception {
        when(req.getMethod()).thenReturn("POST");
        new RequirePostWithGHHookPayload.Processor().shouldBePostMethod(req);
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnNotPost() throws Exception {
        when(req.getMethod()).thenReturn("GET");
        new RequirePostWithGHHookPayload.Processor().shouldBePostMethod(req);
    }

    @Test
    public void shouldPassOnGHEventAndNotBlankPayload() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[]{GHEvent.PUSH, "{}"});
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnNullGHEventAndNotBlankPayload() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[]{null, "{}"});
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnGHEventAndBlankPayload() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[]{GHEvent.PUSH, " "});
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnNulls() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[]{null, null});
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnGreaterCountOfArgs() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[]{GHEvent.PUSH, "{}", " "}
        );
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnLessCountOfArgs() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[]{GHEvent.PUSH}
        );
    }

    @Test
    @Issue("JENKINS-37481")
    public void shouldPassOnAbsentSignatureInRequestIfSecretIsNotConfigured() throws Exception {
        doReturn(PAYLOAD).when(processor).payloadFrom(req, null);
        removeSecret();

        processor.shouldProvideValidSignature(req, null);
    }

    @Test(expected = InvocationTargetException.class)
    @Issue("JENKINS-48012")
    public void shouldNotPassOnAbsentSignatureInRequest() throws Exception {
        doReturn(PAYLOAD).when(processor).payloadFrom(req, null);

        processor.shouldProvideValidSignature(req, null);
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnInvalidSignature() throws Exception {
        final String signature = "sha1=a94a8fe5ccb19ba61c4c0873d391e987982fbbd3";

        when(req.getHeader(RequirePostWithGHHookPayload.Processor.SIGNATURE_HEADER)).thenReturn(signature);
        doReturn(PAYLOAD).when(processor).payloadFrom(req, null);

        processor.shouldProvideValidSignature(req, null);
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnMalformedSignature() throws Exception {
        final String signature = "49d5f5cf800a81f257324912969a2d325d13d3fc";

        when(req.getHeader(RequirePostWithGHHookPayload.Processor.SIGNATURE_HEADER)).thenReturn(signature);
        doReturn(PAYLOAD).when(processor).payloadFrom(req, null);

        processor.shouldProvideValidSignature(req, null);
    }

    @Test
    public void shouldPassWithValidSignature() throws Exception {
        final String signature = "sha1=49d5f5cf800a81f257324912969a2d325d13d3fc";

        when(req.getHeader(RequirePostWithGHHookPayload.Processor.SIGNATURE_HEADER)).thenReturn(signature);
        doReturn(PAYLOAD).when(processor).payloadFrom(req, null);

        processor.shouldProvideValidSignature(req, null);
    }

    @Test
    @Issue("JENKINS-37481")
    public void shouldIgnoreSignHeaderOnNotDefinedSignInConfig() throws Exception {
        removeSecret();
        final String signature = "sha1=49d5f5cf800a81f257324912969a2d325d13d3fc";

        when(req.getHeader(RequirePostWithGHHookPayload.Processor.SIGNATURE_HEADER)).thenReturn(signature);

        processor.shouldProvideValidSignature(req, null);
    }

    @Test
    public void shouldReturnValidPayloadOnApplicationJson() {
        final String payload = "test";

        doReturn(GHEventPayload.PayloadHandler.APPLICATION_JSON).when(req).getContentType();

        final String body = processor.payloadFrom(req, new Object[]{null, payload});

        assertThat("valid returned body", body, equalTo(payload));
    }

    @Test
    public void shouldReturnValidPayloadOnFormUrlEncoded() {
        final String payload = "test";

        doReturn(GHEventPayload.PayloadHandler.FORM_URLENCODED).when(req).getContentType();

        final String body = processor.payloadFrom(req, new Object[]{null, payload});

        assertThat("valid returned body", body, equalTo("payload=" + payload));
    }
}

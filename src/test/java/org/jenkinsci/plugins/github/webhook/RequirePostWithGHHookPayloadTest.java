package org.jenkinsci.plugins.github.webhook;

import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.config.HookSecretConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GHEvent;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.InvocationTargetException;

import static org.mockito.Mockito.*;

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

    @Mock
    private StaplerRequest request;

    @Spy
    private RequirePostWithGHHookPayload.Processor processor;

    @Before
    public void setSecret() {
        Jenkins.getInstance().getDescriptorByType(HookSecretConfig.class).storeSecret(SECRET_CONTENT);
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
                new Object[] {GHEvent.PUSH, " "});
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnNulls() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[] {null, null});
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnGreaterCountOfArgs() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[] {GHEvent.PUSH, "{}", " "}
        );
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnLessCountOfArgs() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[] {GHEvent.PUSH}
        );
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnAbsentSignature() throws Exception {
        doReturn(PAYLOAD).when(processor).readRequestBody(request);

        processor.shouldProvideValidSignature(request);
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnInvalidSignature() throws Exception {
        final String signature = "sha1=a94a8fe5ccb19ba61c4c0873d391e987982fbbd3";

        when(request.getHeader(RequirePostWithGHHookPayload.Processor.SIGNATURE_HEADER)).thenReturn(signature);
        doReturn(PAYLOAD).when(processor).readRequestBody(request);

        processor.shouldProvideValidSignature(request);
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnMalformedSignature() throws Exception {
        final String signature = "49d5f5cf800a81f257324912969a2d325d13d3fc";

        when(request.getHeader(RequirePostWithGHHookPayload.Processor.SIGNATURE_HEADER)).thenReturn(signature);
        doReturn(PAYLOAD).when(processor).readRequestBody(request);

        processor.shouldProvideValidSignature(request);
    }

    @Test
    public void shouldPassWithValidSignature() throws Exception {
        final String signature = "sha1=49d5f5cf800a81f257324912969a2d325d13d3fc";

        when(request.getHeader(RequirePostWithGHHookPayload.Processor.SIGNATURE_HEADER)).thenReturn(signature);
        doReturn(PAYLOAD).when(processor).readRequestBody(request);

        processor.shouldProvideValidSignature(request);
    }
}

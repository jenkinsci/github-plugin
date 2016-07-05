package org.jenkinsci.plugins.github.webhook;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHEvent;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.InvocationTargetException;

import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class RequirePostWithGHHookPayloadTest {

    @Mock
    private StaplerRequest req;

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
                new Object[]{GHEvent.PUSH, "{}", null});
    }

    @Test
    public void shouldPassOnGHEventAndNotBlankPayloadAndValidSignature() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[]{GHEvent.PUSH, "{}", "sha1=3cf05c80c409aeec9416808cf02d4f71c978bb87"});
    }


    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnNullGHEventAndNotBlankPayload() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[]{null, "{}", null});
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnGHEventAndBlankPayload() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[] {GHEvent.PUSH, " ", null});
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnNulls() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[] {null, null, null});
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnGreaterCountOfArgs() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[] {GHEvent.PUSH, "{}", null, " "}
        );
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnLessCountOfArgs() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[] {GHEvent.PUSH}
        );
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnTooShortHash() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[] {GHEvent.PUSH, "{}", "sha1=123"}
        );
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnUnknownHash() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[] {GHEvent.PUSH, "{}", "md5=123"}
        );
    }

    @Test(expected = InvocationTargetException.class)
    public void shouldNotPassOnMalformedHash() throws Exception {
        new RequirePostWithGHHookPayload.Processor().shouldContainParseablePayload(
                new Object[] {GHEvent.PUSH, "{}", " "}
        );
    }
}

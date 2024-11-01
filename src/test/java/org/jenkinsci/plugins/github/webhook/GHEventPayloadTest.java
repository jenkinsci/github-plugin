package org.jenkinsci.plugins.github.webhook;

import com.cloudbees.jenkins.GitHubWebHookFullTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerRequest2;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@RunWith(MockitoJUnitRunner.class)
public class GHEventPayloadTest {

    public static final String NOT_EMPTY_PAYLOAD_CONTENT = "{}";
    public static final String PARAM_NAME = "payload";
    public static final String UNKNOWN_CONTENT_TYPE = "text/plain";

    @Mock
    private StaplerRequest2 req;

    @Mock
    private GHEventPayload ann;

    @Test
    public void shouldReturnPayloadFromForm() throws Exception {
        when(req.getContentType()).thenReturn(GitHubWebHookFullTest.FORM);
        when(req.getParameter(PARAM_NAME)).thenReturn(NOT_EMPTY_PAYLOAD_CONTENT);
        Object payload = new GHEventPayload.PayloadHandler().parse(req, ann, String.class, PARAM_NAME);

        assertThat("class", payload, instanceOf(String.class));
        assertThat("content", (String) payload, equalTo(NOT_EMPTY_PAYLOAD_CONTENT));
    }

    @Test
    public void shouldReturnNullOnUnknownContentType() throws Exception {
        when(req.getContentType()).thenReturn(UNKNOWN_CONTENT_TYPE);
        Object payload = new GHEventPayload.PayloadHandler().parse(req, ann, String.class, PARAM_NAME);

        assertThat("payload should be null", payload, nullValue());
    }
}

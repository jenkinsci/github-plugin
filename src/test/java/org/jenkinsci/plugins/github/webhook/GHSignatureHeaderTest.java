package org.jenkinsci.plugins.github.webhook;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

/**
 * Test for grabbing the X-Hub-Signature header value from an HTTP request.
 * @author lanwen (Merkushev Kirill)
 * @author martinmine
 */
@RunWith(MockitoJUnitRunner.class)
public class GHSignatureHeaderTest {

    private static final String PARAM_NAME = "signature";
    private static final String HEADER_VALUE = "sha1=3cf05c80c409aeec9416808cf02d4f71c978bb87";

    @Mock
    private StaplerRequest req;

    @Mock
    private GHSignatureHeader ann;

    @Test
    public void shouldReturnHeaderWhenPresent() throws Exception {
        when(req.getHeader(GHSignatureHeader.PayloadHandler.SIGNATURE_HEADER)).thenReturn(HEADER_VALUE);

        final Object signature = new GHSignatureHeader.PayloadHandler().parse(req, ann, String.class, PARAM_NAME);
        assertThat("class", signature, instanceOf(String.class));
        assertThat("content", (String) signature, equalTo(HEADER_VALUE));
    }

    @Test
    public void shouldReturnNullWhenNoHeaderIsPresent() throws Exception {
        when(req.getHeader(GHSignatureHeader.PayloadHandler.SIGNATURE_HEADER)).thenReturn(null);

        final Object signature = new GHSignatureHeader.PayloadHandler().parse(req, ann, String.class, PARAM_NAME);
        assertThat("signature should be null", signature, nullValue());
    }
}

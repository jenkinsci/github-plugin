package org.jenkinsci.plugins.github.webhook;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.github.GHEvent;
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
public class GHEventHeaderTest {

    public static final String STRING_PUSH_HEADER = "push";
    public static final String PARAM_NAME = "event";
    public static final String UNKNOWN_EVENT = "unkn";
    
    @Mock
    private StaplerRequest2 req;

    @Mock
    private GHEventHeader ann;

    @Test
    public void shouldReturnParsedPushHeader() throws Exception {
        when(req.getHeader(GHEventHeader.PayloadHandler.EVENT_HEADER)).thenReturn(STRING_PUSH_HEADER);
        Object event = new GHEventHeader.PayloadHandler().parse(req, ann, GHEvent.class, PARAM_NAME);

        assertThat("instance of event", event, instanceOf(GHEvent.class));
        assertThat("parsed event", (GHEvent) event, equalTo(GHEvent.PUSH));
    }

    @Test
    public void shouldReturnNullOnEmptyHeader() throws Exception {
        Object event = new GHEventHeader.PayloadHandler().parse(req, ann, GHEvent.class, PARAM_NAME);

        assertThat("event with empty header", event, nullValue());
    }

    @Test
    public void shouldReturnNullOnUnknownEventHeader() throws Exception {
        when(req.getHeader(GHEventHeader.PayloadHandler.EVENT_HEADER)).thenReturn(UNKNOWN_EVENT);
        Object event = new GHEventHeader.PayloadHandler().parse(req, ann, GHEvent.class, PARAM_NAME);

        assertThat("event with unknown event header", event, nullValue());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExcOnWrongTypeOfHeader() throws Exception {
        new GHEventHeader.PayloadHandler().parse(req, ann, String.class, PARAM_NAME);
    }
}

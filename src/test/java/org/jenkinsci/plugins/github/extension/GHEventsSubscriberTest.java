package org.jenkinsci.plugins.github.extension;

import hudson.model.AbstractProject;
import org.junit.Test;
import org.kohsuke.github.GHEvent;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GHEventsSubscriberTest {

    @Test
    public void shouldReturnEmptySetInsteadOfNull() throws Exception {
        Set<GHEvent> set = GHEventsSubscriber.extractEvents().apply(new NullSubscriber());
        assertThat("null should be replaced", set, hasSize(0));
    }

    @Test
    public void shouldMatchAgainstEmptySetInsteadOfNull() throws Exception {
        boolean result = GHEventsSubscriber.isInterestedIn(GHEvent.PUSH).apply(new NullSubscriber());
        assertThat("null should be replaced", result, is(false));
    }

    public static class NullSubscriber extends GHEventsSubscriber {
        @Override
        protected boolean isApplicable(AbstractProject<?, ?> project) {
            return true;
        }

        @Override
        protected Set<GHEvent> events() {
            return null;
        }
    }
}

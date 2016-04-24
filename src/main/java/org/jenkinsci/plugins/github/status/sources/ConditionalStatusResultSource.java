package org.jenkinsci.plugins.github.status.sources;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang3.EnumUtils;
import org.jenkinsci.plugins.github.common.ExpandableMessage;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusResultSource;
import org.jenkinsci.plugins.github.extension.status.misc.ConditionalResult;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.kohsuke.github.GHCommitState.ERROR;
import static org.kohsuke.github.GHCommitState.PENDING;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class ConditionalStatusResultSource extends GitHubStatusResultSource {

    private List<ConditionalResult> results;

    @DataBoundConstructor
    public ConditionalStatusResultSource(List<ConditionalResult> results) {
        this.results = results;
    }

    @Nonnull
    public List<ConditionalResult> getResults() {
        return defaultIfNull(results, Collections.<ConditionalResult>emptyList());
    }

    /**
     * First matching result win. Or will be used pending state.
     * Messages are expanded with token macro and env variables
     *
     * @return first matched result or pending state with warn msg
     */
    @Override
    public StatusResult get(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener)
            throws IOException, InterruptedException {

        for (ConditionalResult conditionalResult : getResults()) {
            if (conditionalResult.matches(run)) {
                return new StatusResult(
                        defaultIfNull(EnumUtils.getEnum(GHCommitState.class, conditionalResult.getState()), ERROR),
                        new ExpandableMessage(conditionalResult.getMessage()).expandAll(run, listener)
                );
            }
        }

        return new StatusResult(
                PENDING,
                new ExpandableMessage("Can't define which status to set").expandAll(run, listener)
        );
    }

    @Extension
    public static class ConditionalStatusResultSourceDescriptor extends Descriptor<GitHubStatusResultSource> {
        @Override
        public String getDisplayName() {
            return "Based on build result manually defined";
        }
    }

}

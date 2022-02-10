package org.jenkinsci.plugins.github.status.sources;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.common.ExpandableMessage;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusContextSource;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Allows to manually enter context
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public class ManuallyEnteredCommitContextSource extends GitHubStatusContextSource {
    private static final Logger LOG = LoggerFactory.getLogger(ManuallyEnteredCommitContextSource.class);

    private String context;

    @DataBoundConstructor
    public ManuallyEnteredCommitContextSource(String context) {
        this.context = context;
    }

    public String getContext() {
        return context;
    }

    /**
     * Just returns what user entered. Expands env vars and token macro
     */
    @Override
    public String context(@NonNull Run<?, ?> run, @NonNull TaskListener listener) {
        try {
            return new ExpandableMessage(context).expandAll(run, listener);
        } catch (Exception e) {
            LOG.debug("Can't expand context, returning as is", e);
            return context;
        }
    }

    @Extension
    public static class ManuallyEnteredCommitContextSourceDescriptor extends Descriptor<GitHubStatusContextSource> {
        @Override
        public String getDisplayName() {
            return "Manually entered context name";
        }
    }
}

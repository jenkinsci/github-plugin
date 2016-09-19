package org.jenkinsci.plugins.github.status.sources;

import org.jenkinsci.plugins.github.common.ExpandableMessage;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusBackrefSource;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * Allows to manually enter backref, with env/token expansion.
 *
 * @author pupssman (Kalinin Ivan)
 * @since 1.21.2
 *
 */
public class ManuallyEnteredBackrefSource extends GitHubStatusBackrefSource {
    private static final Logger LOG = LoggerFactory.getLogger(ManuallyEnteredBackrefSource.class);

    private String backref;

    @DataBoundConstructor
    public ManuallyEnteredBackrefSource(String backref) {
        this.backref = backref;
    }

    public String getBackref() {
        return backref;
    }

    /**
     * Just returns what user entered. Expands env vars and token macro
     */
    @SuppressWarnings("deprecation")
    @Override
    public String get(Run<?, ?> run, TaskListener listener) {
        try {
            return new ExpandableMessage(backref).expandAll(run, listener);
        } catch (Exception e) {
            LOG.debug("Can't expand backref, returning as is", e);
            return backref;
        }
    }

    @Extension
    public static class ManuallyEnteredBackrefSourceDescriptor extends Descriptor<GitHubStatusBackrefSource> {
        @Override
        public String getDisplayName() {
            return "Manually entered backref";
        }
    }

}

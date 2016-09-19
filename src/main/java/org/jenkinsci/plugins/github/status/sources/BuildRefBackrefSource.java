package org.jenkinsci.plugins.github.status.sources;

import org.jenkinsci.plugins.github.extension.status.GitHubStatusBackrefSource;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * Gets backref from Run URL.
 *
 * @author pupssman (Kalinin Ivan)
 * @since 1.21.2
 *
 */
public class BuildRefBackrefSource extends GitHubStatusBackrefSource {

    /**
     * Returns absolute URL of the Run
     */
    @SuppressWarnings("deprecation")
    @Override
    public String get(Run<?, ?> run, TaskListener listener) {
        return run.getAbsoluteUrl();
    }

    @Extension
    public static class BuildRefBackrefSourceDescriptor extends Descriptor<GitHubStatusBackrefSource> {
        @Override
        public String getDisplayName() {
            return "Backref to the build";
        }
    }
}

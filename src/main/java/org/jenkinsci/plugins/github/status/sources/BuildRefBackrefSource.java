package org.jenkinsci.plugins.github.status.sources;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusBackrefSource;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Gets backref from Run URL.
 *
 * @author pupssman (Kalinin Ivan)
 * @since 1.22.1
 */
public class BuildRefBackrefSource extends GitHubStatusBackrefSource {

    @DataBoundConstructor
    public BuildRefBackrefSource() {
    }

    /**
     * Returns absolute URL of the Run
     */
    @SuppressWarnings("deprecation")
    @Override
    public String get(Run<?, ?> run, TaskListener listener) {
        return DisplayURLProvider.get().getRunURL(run);
    }

    @Extension
    public static class BuildRefBackrefSourceDescriptor extends Descriptor<GitHubStatusBackrefSource> {
        @Override
        public String getDisplayName() {
            return "Backref to the build";
        }
    }
}

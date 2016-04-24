package org.jenkinsci.plugins.github.status.sources;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusContextSource;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

import static com.coravy.hudson.plugins.github.GithubProjectProperty.displayNameFor;

/**
 * Uses job name or defined in prop context name
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public class DefaultCommitContextSource extends GitHubStatusContextSource {

    @DataBoundConstructor
    public DefaultCommitContextSource() {
    }

    /**
     * @return context name
     * @see com.coravy.hudson.plugins.github.GithubProjectProperty#displayNameFor(Job)
     */
    @Override
    public String context(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener) {
        return displayNameFor(run.getParent());
    }

    @Extension
    public static class DefaultContextSourceDescriptor extends Descriptor<GitHubStatusContextSource> {
        @Override
        public String getDisplayName() {
            return "From GitHub property with fallback to job name";
        }
    }
}

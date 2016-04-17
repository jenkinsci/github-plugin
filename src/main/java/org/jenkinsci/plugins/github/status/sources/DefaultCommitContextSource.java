package org.jenkinsci.plugins.github.status.sources;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusContextSource;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

import static com.coravy.hudson.plugins.github.GithubProjectProperty.displayNameFor;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class DefaultCommitContextSource extends GitHubStatusContextSource {
    
    @DataBoundConstructor
    public DefaultCommitContextSource() {
    }

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

package org.jenkinsci.plugins.github.status.sources;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusContextSource;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class ManuallyEnteredCommitContextSource extends GitHubStatusContextSource {
    private String context;
    
    @DataBoundConstructor
    public ManuallyEnteredCommitContextSource(String context) {
        this.context = context;
    }

    @Override
    public String context(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener) {
        return context;
    }

    @Extension
    public static class ManuallyEnteredCommitContextSourceDescriptor extends Descriptor<GitHubStatusContextSource> {
        @Override
        public String getDisplayName() {
            return "Manually entered context name";
        }
    }
}

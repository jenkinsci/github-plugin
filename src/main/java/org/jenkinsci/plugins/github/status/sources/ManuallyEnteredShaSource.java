package org.jenkinsci.plugins.github.status.sources;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.common.ExpandableMessage;
import org.jenkinsci.plugins.github.extension.status.GitHubCommitShaSource;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class ManuallyEnteredShaSource extends GitHubCommitShaSource {

    private String sha;

    @DataBoundConstructor
    public ManuallyEnteredShaSource(String sha) {
        this.sha = sha;
    }

    public String getSha() {
        return sha;
    }

    @Override
    public String get(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        return new ExpandableMessage(sha).expandAll(run, listener);
    }

    @Extension
    public static class ManuallyEnteredShaSourceDescriptor extends Descriptor<GitHubCommitShaSource> {
        @Override
        public String getDisplayName() {
            return "Manually entered SHA";
        }
    }
}

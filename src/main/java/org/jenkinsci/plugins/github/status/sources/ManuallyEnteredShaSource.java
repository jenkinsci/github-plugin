package org.jenkinsci.plugins.github.status.sources;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.common.ExpandableMessage;
import org.jenkinsci.plugins.github.extension.status.GitHubCommitShaSource;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

/**
 * Allows to enter sha manually
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
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

    /**
     * Expands env vars and token macro in entered sha
     */
    @Override
    public String get(@NonNull Run<?, ?> run, @NonNull TaskListener listener) throws IOException, InterruptedException {
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

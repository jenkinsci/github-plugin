package org.jenkinsci.plugins.github.status.sources;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.github.extension.status.GitHubCommitShaSource;
import org.jenkinsci.plugins.github.util.BuildDataHelper;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class BuildDataRevisionShaSource extends GitHubCommitShaSource {

    @DataBoundConstructor
    public BuildDataRevisionShaSource() {
    }

    @Override
    public String get(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener) throws IOException {
        return ObjectId.toString(BuildDataHelper.getCommitSHA1(run));
    }

    @Extension
    public static class BuildDataRevisionShaSourceDescriptor extends Descriptor<GitHubCommitShaSource> {
        @Override
        public String getDisplayName() {
            return "Latest build revision";
        }
    }
}

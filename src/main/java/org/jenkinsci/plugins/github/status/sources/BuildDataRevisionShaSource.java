package org.jenkinsci.plugins.github.status.sources;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.github.extension.status.GitHubCommitShaSource;
import org.jenkinsci.plugins.github.util.BuildDataHelper;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

/**
 * Gets sha from build data
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public class BuildDataRevisionShaSource extends GitHubCommitShaSource {

    @DataBoundConstructor
    public BuildDataRevisionShaSource() {
    }

    /**
     * @return sha from git's scm build data action
     */
    @Override
    public String get(@NonNull Run<?, ?> run, @NonNull TaskListener listener) throws IOException {
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

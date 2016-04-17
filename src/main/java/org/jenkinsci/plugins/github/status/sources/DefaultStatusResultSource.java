package org.jenkinsci.plugins.github.status.sources;

import com.cloudbees.jenkins.Messages;
import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusResultSource;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class DefaultStatusResultSource extends GitHubStatusResultSource {

    @DataBoundConstructor
    public DefaultStatusResultSource() {
    }

    @Override
    public StatusResult get(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener) {
        Result result = run.getResult();

        // We do not use `build.getDurationString()` because it appends 'and counting' (build is still running)
        String duration = Util.getTimeSpanString(System.currentTimeMillis() - run.getTimeInMillis());

        if (result == null) { // Build is ongoing
            return new GitHubStatusResultSource.StatusResult(
                    GHCommitState.PENDING,
                    Messages.CommitNotifier_Pending(run.getDisplayName())
            );
        } else if (result.isBetterOrEqualTo(SUCCESS)) {
            return new GitHubStatusResultSource.StatusResult(
                    GHCommitState.SUCCESS,
                    Messages.CommitNotifier_Success(run.getDisplayName(), duration)
            );
        } else if (result.isBetterOrEqualTo(UNSTABLE)) {
            return new GitHubStatusResultSource.StatusResult(
                    GHCommitState.FAILURE,
                    Messages.CommitNotifier_Unstable(run.getDisplayName(), duration)
            );
        } else {
            return new GitHubStatusResultSource.StatusResult(
                    GHCommitState.ERROR,
                    Messages.CommitNotifier_Failed(run.getDisplayName(), duration)
            );
        }
    }

    @Extension
    public static class DefaultResultSourceDescriptor extends Descriptor<GitHubStatusResultSource> {
        @Override
        public String getDisplayName() {
            return "One of default messages and statuses";
        }
    }
}

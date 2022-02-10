package org.jenkinsci.plugins.github.status.sources;

import com.cloudbees.jenkins.Messages;
import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusResultSource;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;

import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;
import static java.util.Arrays.asList;
import static org.jenkinsci.plugins.github.status.sources.misc.AnyBuildResult.onAnyResult;
import static org.jenkinsci.plugins.github.status.sources.misc.BetterThanOrEqualBuildResult.betterThanOrEqualTo;

/**
 * Default way to report about build results.
 * Reports about time and build status
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public class DefaultStatusResultSource extends GitHubStatusResultSource {

    @DataBoundConstructor
    public DefaultStatusResultSource() {
    }

    @Override
    public StatusResult get(@NonNull Run<?, ?> run, @NonNull TaskListener listener) throws IOException,
            InterruptedException {

        // We do not use `build.getDurationString()` because it appends 'and counting' (build is still running)
        String duration = Util.getTimeSpanString(System.currentTimeMillis() - run.getTimeInMillis());

        return new ConditionalStatusResultSource(asList(
                betterThanOrEqualTo(SUCCESS,
                        GHCommitState.SUCCESS, Messages.CommitNotifier_Success(run.getDisplayName(), duration)),

                betterThanOrEqualTo(UNSTABLE,
                        GHCommitState.FAILURE, Messages.CommitNotifier_Unstable(run.getDisplayName(), duration)),

                betterThanOrEqualTo(FAILURE,
                        GHCommitState.ERROR, Messages.CommitNotifier_Failed(run.getDisplayName(), duration)),

                onAnyResult(GHCommitState.PENDING, Messages.CommitNotifier_Pending(run.getDisplayName()))
        )).get(run, listener);
    }

    @Extension
    public static class DefaultResultSourceDescriptor extends Descriptor<GitHubStatusResultSource> {
        @Override
        public String getDisplayName() {
            return "One of default messages and statuses";
        }
    }
}

package com.cloudbees.jenkins;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.github.util.BuildDataHelper;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

import static com.cloudbees.jenkins.Messages.GitHubCommitNotifier_SettingCommitStatus;
import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;
import static java.lang.String.format;

/**
 * Create commit status notifications on the commits based on the outcome of the build.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GitHubCommitNotifier extends Notifier {

    private final String resultOnFailure;
    private static final Result[] SUPPORTED_RESULTS = {FAILURE, UNSTABLE, SUCCESS};

    /**
     * @since 1.10
     */
    @DataBoundConstructor
    public GitHubCommitNotifier(String resultOnFailure) {
        this.resultOnFailure = resultOnFailure;
    }

    @Deprecated
    public GitHubCommitNotifier() {
        this(getDefaultResultOnFailure().toString());
    }

    /**
     * @since 1.10
     */
    @Nonnull
    public String getResultOnFailure() {
        return resultOnFailure != null ? resultOnFailure : getDefaultResultOnFailure().toString();
    }

    @Nonnull
    public static Result getDefaultResultOnFailure() {
        return SUPPORTED_RESULTS[0];
    }


    @Nonnull
    /*package*/ Result getEffectiveResultOnFailure() {
        if (resultOnFailure == null) {
            return getDefaultResultOnFailure();
        }

        for (Result result : SUPPORTED_RESULTS) {
            if (result.toString().equals(resultOnFailure)) {
                return result;
            }
        }
        return getDefaultResultOnFailure();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build,
                           Launcher launcher,
                           BuildListener listener) throws InterruptedException, IOException {
        try {
            updateCommitStatus(build, listener);
            return true;
        } catch (IOException error) {
            final Result buildResult = getEffectiveResultOnFailure();
            if (buildResult.equals(Result.FAILURE)) {
                throw error;
            } else {
                listener.error(format("[GitHub Commit Notifier] - %s", error.getMessage()));
                if (buildResult.isWorseThan(build.getResult())) {
                    listener.getLogger().println(
                            format("[GitHub Commit Notifier] - Build result will be set to %s", buildResult)
                    );
                    build.setResult(buildResult);
                }
            }
        }
        return true;
    }

    private void updateCommitStatus(@Nonnull AbstractBuild<?, ?> build,
                                    @Nonnull BuildListener listener) throws InterruptedException, IOException {
        final String sha1 = ObjectId.toString(BuildDataHelper.getCommitSHA1(build));
        for (GitHubRepositoryName name : GitHubRepositoryNameContributor.parseAssociatedNames(build.getProject())) {
            for (GHRepository repository : name.resolve()) {
                GHCommitState state;
                String msg;

                // We do not use `build.getDurationString()` because it appends 'and counting' (build is still running)
                final String duration = Util.getTimeSpanString(System.currentTimeMillis() - build.getTimeInMillis());

                Result result = build.getResult();
                if (result == null) { // Build is ongoing
                    state = GHCommitState.PENDING;
                    msg = Messages.CommitNotifier_Pending(build.getDisplayName());
                } else if (result.isBetterOrEqualTo(SUCCESS)) {
                    state = GHCommitState.SUCCESS;
                    msg = Messages.CommitNotifier_Success(build.getDisplayName(), duration);
                } else if (result.isBetterOrEqualTo(UNSTABLE)) {
                    state = GHCommitState.FAILURE;
                    msg = Messages.CommitNotifier_Unstable(build.getDisplayName(), duration);
                } else {
                    state = GHCommitState.ERROR;
                    msg = Messages.CommitNotifier_Failed(build.getDisplayName(), duration);
                }

                listener.getLogger().println(
                        GitHubCommitNotifier_SettingCommitStatus(repository.getHtmlUrl() + "/commit/" + sha1)
                );
                repository.createCommitStatus(
                        sha1, state, build.getAbsoluteUrl(), msg, build.getProject().getFullName());
            }
        }
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Set build status on GitHub commit";
        }

        public ListBoxModel doFillResultOnFailureItems() {
            ListBoxModel items = new ListBoxModel();
            for (Result result : SUPPORTED_RESULTS) {
                items.add(result.toString());
            }
            return items;
        }
    }

}

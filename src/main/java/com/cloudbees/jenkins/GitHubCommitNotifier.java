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
import org.jenkinsci.plugins.github.common.ExpandableMessage;
import org.jenkinsci.plugins.github.util.BuildDataHelper;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;

import static com.cloudbees.jenkins.Messages.GitHubCommitNotifier_DisplayName;
import static com.cloudbees.jenkins.Messages.GitHubCommitNotifier_SettingCommitStatus;
import static com.coravy.hudson.plugins.github.GithubProjectProperty.displayNameFor;
import static com.google.common.base.Objects.firstNonNull;
import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * Create commit status notifications on the commits based on the outcome of the build.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GitHubCommitNotifier extends Notifier {
    private static final ExpandableMessage DEFAULT_MESSAGE = new ExpandableMessage("");

    private ExpandableMessage statusMessage = DEFAULT_MESSAGE;

    private final String resultOnFailure;
    private static final Result[] SUPPORTED_RESULTS = {FAILURE, UNSTABLE, SUCCESS};

    @Restricted(NoExternalUse.class)
    public GitHubCommitNotifier() {
        this(getDefaultResultOnFailure().toString());
    }

    /**
     * @since 1.10
     */
    @DataBoundConstructor
    public GitHubCommitNotifier(String resultOnFailure) {
        this.resultOnFailure = resultOnFailure;
    }

    /**
     * @since 1.14.1
     */
    public ExpandableMessage getStatusMessage() {
        return statusMessage;
    }

    /**
     * @since 1.14.1
     */
    @DataBoundSetter
    public void setStatusMessage(ExpandableMessage statusMessage) {
        this.statusMessage = statusMessage;
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
        return FAILURE;
    }

    @Nonnull
    /*package*/ Result getEffectiveResultOnFailure() {
        return Result.fromString(trimToEmpty(resultOnFailure));
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
            if (buildResult.equals(FAILURE)) {
                throw error;
            } else {
                listener.error(format("[GitHub Commit Notifier] - %s", error.getMessage()));
                listener.getLogger().println(
                        format("[GitHub Commit Notifier] - Build result will be set to %s", buildResult)
                );
                build.setResult(buildResult);
            }
        }
        return true;
    }

    private void updateCommitStatus(@Nonnull AbstractBuild<?, ?> build,
                                    @Nonnull BuildListener listener) throws InterruptedException, IOException {
        final String sha1 = ObjectId.toString(BuildDataHelper.getCommitSHA1(build));

        StatusResult status = statusFrom(build);
        String message = defaultIfEmpty(firstNonNull(statusMessage, DEFAULT_MESSAGE)
                .expandAll(build, listener), status.getMsg());
        String contextName = displayNameFor(build.getProject());

        for (GitHubRepositoryName name : GitHubRepositoryNameContributor.parseAssociatedNames(build.getProject())) {
            for (GHRepository repository : name.resolve()) {

                listener.getLogger().println(
                        GitHubCommitNotifier_SettingCommitStatus(repository.getHtmlUrl() + "/commit/" + sha1)
                );

                repository.createCommitStatus(
                        sha1, status.getState(), build.getAbsoluteUrl(),
                        message,
                        contextName
                );
            }
        }
    }

    private static StatusResult statusFrom(@Nonnull AbstractBuild<?, ?> build) {
        Result result = build.getResult();

        // We do not use `build.getDurationString()` because it appends 'and counting' (build is still running)
        String duration = Util.getTimeSpanString(System.currentTimeMillis() - build.getTimeInMillis());

        if (result == null) { // Build is ongoing
            return new StatusResult(
                    GHCommitState.PENDING,
                    Messages.CommitNotifier_Pending(build.getDisplayName())
            );
        } else if (result.isBetterOrEqualTo(SUCCESS)) {
            return new StatusResult(
                    GHCommitState.SUCCESS,
                    Messages.CommitNotifier_Success(build.getDisplayName(), duration)
            );
        } else if (result.isBetterOrEqualTo(UNSTABLE)) {
            return new StatusResult(
                    GHCommitState.FAILURE,
                    Messages.CommitNotifier_Unstable(build.getDisplayName(), duration)
            );
        } else {
            return new StatusResult(
                    GHCommitState.ERROR,
                    Messages.CommitNotifier_Failed(build.getDisplayName(), duration)
            );
        }
    }

    private static class StatusResult {
        private GHCommitState state;
        private String msg;

        public StatusResult(GHCommitState state, String msg) {
            this.state = state;
            this.msg = msg;
        }

        public GHCommitState getState() {
            return state;
        }

        public String getMsg() {
            return msg;
        }
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return GitHubCommitNotifier_DisplayName();
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

package com.cloudbees.jenkins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.plugins.github.common.ExpandableMessage;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusContextSource;
import org.jenkinsci.plugins.github.extension.status.StatusErrorHandler;
import org.jenkinsci.plugins.github.status.GitHubCommitStatusSetter;
import org.jenkinsci.plugins.github.status.err.ChangingBuildStatusErrorHandler;
import org.jenkinsci.plugins.github.status.err.ShallowAnyErrorHandler;
import org.jenkinsci.plugins.github.status.sources.AnyDefinedRepositorySource;
import org.jenkinsci.plugins.github.status.sources.BuildDataRevisionShaSource;
import org.jenkinsci.plugins.github.status.sources.ConditionalStatusResultSource;
import org.jenkinsci.plugins.github.status.sources.DefaultCommitContextSource;
import org.jenkinsci.plugins.github.status.sources.DefaultStatusResultSource;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;

import static com.cloudbees.jenkins.Messages.GitHubCommitNotifier_DisplayName;
import static com.google.common.base.Objects.firstNonNull;
import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.jenkinsci.plugins.github.status.sources.misc.AnyBuildResult.onAnyResult;
import static org.jenkinsci.plugins.github.status.sources.misc.BetterThanOrEqualBuildResult.betterThanOrEqualTo;

/**
 * Create commit status notifications on the commits based on the outcome of the build.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GitHubCommitNotifier extends Notifier implements SimpleBuildStep {
    private static final ExpandableMessage DEFAULT_MESSAGE = new ExpandableMessage("");

    private ExpandableMessage statusMessage = DEFAULT_MESSAGE;
    private GitHubStatusContextSource contextSource = new DefaultCommitContextSource();

    private final String resultOnFailure;
    private static final Result[] SUPPORTED_RESULTS = {FAILURE, UNSTABLE, SUCCESS};

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubCommitNotifier.class);

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
     * @return Context provider
     * @since 1.24.0
     */
    public GitHubStatusContextSource getContextSource() {
        return contextSource;
    }

    /**
     * @since 1.14.1
     */
    public ExpandableMessage getStatusMessage() {
        return statusMessage;
    }

    /**
     * @since 1.24.0
     */
    @DataBoundSetter
    public void setContextSource(GitHubStatusContextSource contextSource) {
        this.contextSource = contextSource;
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

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public void perform(@NonNull Run<?, ?> build,
                        @NonNull FilePath ws,
                        @NonNull Launcher launcher,
                        @NonNull TaskListener listener) throws InterruptedException, IOException {

        GitHubCommitStatusSetter setter = new GitHubCommitStatusSetter();
        setter.setReposSource(new AnyDefinedRepositorySource());
        setter.setCommitShaSource(new BuildDataRevisionShaSource());
        setter.setContextSource(contextSource);


        String content = firstNonNull(statusMessage, DEFAULT_MESSAGE).getContent();

        if (isNotBlank(content)) {
            setter.setStatusResultSource(new ConditionalStatusResultSource(
                    asList(
                            betterThanOrEqualTo(SUCCESS, GHCommitState.SUCCESS, content),
                            betterThanOrEqualTo(UNSTABLE, GHCommitState.FAILURE, content),
                            betterThanOrEqualTo(FAILURE, GHCommitState.ERROR, content),
                            onAnyResult(GHCommitState.PENDING, content)
                    )));
        } else {
            setter.setStatusResultSource(new DefaultStatusResultSource());
        }

        if (getEffectiveResultOnFailure().equals(SUCCESS)) {
            setter.setErrorHandlers(Collections.<StatusErrorHandler>singletonList(new ShallowAnyErrorHandler()));
        } else if (resultOnFailure == null) {
            setter.setErrorHandlers(null);
        } else {
            setter.setErrorHandlers(Collections.<StatusErrorHandler>singletonList(
                    new ChangingBuildStatusErrorHandler(getEffectiveResultOnFailure().toString())));
        }

        setter.perform(build, ws, launcher, listener);
    }

    public Object readResolve() {
        if (getContextSource() == null) {
            setContextSource(new DefaultCommitContextSource());
        }
        return this;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
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

package com.cloudbees.jenkins;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.plugins.github.common.ExpandableMessage;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusBackrefSource;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusContextSource;
import org.jenkinsci.plugins.github.extension.status.StatusErrorHandler;
import org.jenkinsci.plugins.github.extension.status.misc.ConditionalResult;
import org.jenkinsci.plugins.github.status.GitHubCommitStatusSetter;
import org.jenkinsci.plugins.github.status.err.ShallowAnyErrorHandler;
import org.jenkinsci.plugins.github.status.sources.AnyDefinedRepositorySource;
import org.jenkinsci.plugins.github.status.sources.BuildDataRevisionShaSource;
import org.jenkinsci.plugins.github.status.sources.ConditionalStatusResultSource;
import org.jenkinsci.plugins.github.status.sources.DefaultCommitContextSource;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.util.Collections;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.jenkinsci.plugins.github.status.sources.misc.AnyBuildResult.onAnyResult;

@Extension
public class GitHubSetCommitStatusBuilder extends Builder implements SimpleBuildStep {
    private static final ExpandableMessage DEFAULT_MESSAGE = new ExpandableMessage("");

    private ExpandableMessage statusMessage = DEFAULT_MESSAGE;
    private GitHubStatusContextSource contextSource = new DefaultCommitContextSource();
    private GitHubStatusBackrefSource statusBackrefSource;

    @DataBoundConstructor
    public GitHubSetCommitStatusBuilder() {
    }

    /**
     * @since 1.14.1
     */
    public ExpandableMessage getStatusMessage() {
        return statusMessage;
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
    @DataBoundSetter
    public void setStatusMessage(ExpandableMessage statusMessage) {
        this.statusMessage = statusMessage;
    }

    /**
     * @since 1.24.0
     */
    @DataBoundSetter
    public void setContextSource(GitHubStatusContextSource contextSource) {
        this.contextSource = contextSource;
    }


    @DataBoundSetter
    public void setStatusBackrefSource(GitHubStatusBackrefSource statusBackrefSource) {
        this.statusBackrefSource = statusBackrefSource;
    }

    @Override
    public void perform(@NonNull Run<?, ?> build,
                        @NonNull FilePath workspace,
                        @NonNull Launcher launcher,
                        @NonNull TaskListener listener) throws InterruptedException, IOException {

        GitHubCommitStatusSetter setter = new GitHubCommitStatusSetter();
        setter.setReposSource(new AnyDefinedRepositorySource());
        setter.setCommitShaSource(new BuildDataRevisionShaSource());
        setter.setContextSource(contextSource);
        setter.setErrorHandlers(Collections.<StatusErrorHandler>singletonList(new ShallowAnyErrorHandler()));
        setter.setStatusBackrefSource(statusBackrefSource);

        setter.setStatusResultSource(new ConditionalStatusResultSource(
                Collections.<ConditionalResult>singletonList(
                        onAnyResult(
                                GHCommitState.PENDING,
                                defaultIfEmpty((statusMessage != null ? statusMessage : DEFAULT_MESSAGE).getContent(),
                                        Messages.CommitNotifier_Pending(build.getDisplayName()))
                        )
                )));

        setter.perform(build, workspace, launcher, listener);
    }


    public Object readResolve() {
        if (getContextSource() == null) {
            setContextSource(new DefaultCommitContextSource());
        }
        return this;
    }

    @Extension
    public static class Descriptor extends BuildStepDescriptor<Builder> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.GitHubSetCommitStatusBuilder_DisplayName();
        }
    }
}

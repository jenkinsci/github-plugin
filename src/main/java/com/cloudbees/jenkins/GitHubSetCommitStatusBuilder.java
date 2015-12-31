package com.cloudbees.jenkins;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;

import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.github.common.ExpandableMessage;
import org.jenkinsci.plugins.github.util.BuildDataHelper;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

import static com.cloudbees.jenkins.Messages.GitHubCommitNotifier_SettingCommitStatus;
import static com.coravy.hudson.plugins.github.GithubProjectProperty.displayNameFor;
import static com.google.common.base.Objects.firstNonNull;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

@Extension
public class GitHubSetCommitStatusBuilder extends Builder implements SimpleBuildStep {
    private static final ExpandableMessage DEFAULT_MESSAGE = new ExpandableMessage("");

    private ExpandableMessage statusMessage = DEFAULT_MESSAGE;

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
     * @since 1.14.1
     */
    @DataBoundSetter
    public void setStatusMessage(ExpandableMessage statusMessage) {
        this.statusMessage = statusMessage;
    }

    @Override
    public void perform(Run<?, ?> build,
                        FilePath workspace,
                        Launcher launcher,
                        TaskListener listener) throws InterruptedException, IOException {
        final String sha1 = ObjectId.toString(BuildDataHelper.getCommitSHA1(build));
        String message = defaultIfEmpty(
                firstNonNull(statusMessage, DEFAULT_MESSAGE).expandAll(build, listener),
                Messages.CommitNotifier_Pending(build.getDisplayName())
        );
        String contextName = displayNameFor(build.getParent());

        for (GitHubRepositoryName name : GitHubRepositoryNameContributor.parseAssociatedNames(build.getParent())) {
            for (GHRepository repository : name.resolve()) {
                listener.getLogger().println(
                        GitHubCommitNotifier_SettingCommitStatus(repository.getHtmlUrl() + "/commit/" + sha1)
                );
                repository.createCommitStatus(sha1,
                        GHCommitState.PENDING,
                        build.getAbsoluteUrl(),
                        message,
                        contextName);
            }
        }
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

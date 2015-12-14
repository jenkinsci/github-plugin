package com.cloudbees.jenkins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
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
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

@Extension
public class GitHubSetCommitStatusBuilder extends Builder {
    private ExpandableMessage statusMessage = new ExpandableMessage("");

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
    public boolean perform(AbstractBuild<?, ?> build,
                           Launcher launcher,
                           BuildListener listener) throws InterruptedException, IOException {
        final String sha1 = ObjectId.toString(BuildDataHelper.getCommitSHA1(build));
        String message = defaultIfEmpty(
                statusMessage.expandAll(build, listener),
                Messages.CommitNotifier_Pending(build.getDisplayName())
        );
        String contextName = displayNameFor(build.getProject());

        for (GitHubRepositoryName name : GitHubRepositoryNameContributor.parseAssociatedNames(build.getProject())) {
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
        return true;
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

package com.cloudbees.jenkins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.tasks.Builder;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.model.AbstractProject;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.plugins.git.util.BuildData;
import org.eclipse.jgit.lib.ObjectId;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import org.jenkinsci.plugins.github.util.BuildDataHelper;

@Extension
public class GitHubSetCommitStatusBuilder extends Builder {
    @DataBoundConstructor
    public GitHubSetCommitStatusBuilder() {
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {     
        final String sha1 = ObjectId.toString(BuildDataHelper.getCommitSHA1(build)); 
        for (GitHubRepositoryName name : GitHubRepositoryNameContributor.parseAssociatedNames(build.getProject())) {
            for (GHRepository repository : name.resolve()) {
                listener.getLogger().println(Messages.GitHubCommitNotifier_SettingCommitStatus(repository.getUrl() + "/commit/" + sha1));
                repository.createCommitStatus(sha1, GHCommitState.PENDING, build.getAbsoluteUrl(), Messages.CommitNotifier_Pending(build.getDisplayName()));
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
            return "Set build status to \"pending\" on GitHub commit";
        }
    }
}
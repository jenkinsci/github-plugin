package com.cloudbees.jenkins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.util.BuildData;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.multiplescms.MultiSCM;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

import static hudson.model.Result.*;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class GitHubCommitNotifier extends Notifier {


    @DataBoundConstructor
    public GitHubCommitNotifier() {
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        BuildData buildData = build.getAction(BuildData.class);
        String sha1 = ObjectId.toString(buildData.getLastBuiltRevision().getSha1());

        GitHubTrigger trigger = (GitHubTrigger) build.getProject().getTrigger(GitHubPushTrigger.class);
        if(trigger != null && !trigger.getGitHubRepositories().isEmpty()){
            return performByTrigger(trigger,build,listener,sha1);
        }else{
            return performBySCM(build.getProject().getScm(), build, listener, sha1);
        }
    }

    private boolean performBySCM(SCM scm,AbstractBuild<?, ?> build, BuildListener listener, String sha1) throws InterruptedException, IOException {
        if (Hudson.getInstance().getPlugin("multiple-scms") != null&& scm instanceof MultiSCM) {
            throw new  InterruptedException ("multiple-scms not supported for polling, please use Github trigger");
        }
        if (scm instanceof GitSCM) {
            GitSCM gitSCM = (GitSCM) scm;
            for (UserRemoteConfig urc: gitSCM.getUserRemoteConfigs()){
                GitHubSCM gitHubPolling = GitHubSCM.create(urc.getUrl());
                GHRepository repository = gitHubPolling.getGitHubRepo();
                createStatus(repository, build, listener,sha1);
            }
        }
        return true;
    }

    private boolean performByTrigger(GitHubTrigger trigger, AbstractBuild<?, ?> build,BuildListener listener,String sha1) throws IOException{
        for (GitHubRepositoryName gitHubRepositoryName : trigger.getGitHubRepositories()) {
            for (GHRepository repository : gitHubRepositoryName.resolve()) {
                createStatus(repository, build, listener,sha1);
            }
        }
        return true;
    }

    private void createStatus(GHRepository repository, AbstractBuild<?, ?> build, BuildListener listener, String sha1) throws IOException {
        GHCommitState state;
        String msg;

        Result result = build.getResult();
        if (result.isBetterOrEqualTo(SUCCESS)) {
            state = GHCommitState.SUCCESS;
            msg = "Success";
        } else if (result.isBetterOrEqualTo(UNSTABLE)) {
            state = GHCommitState.FAILURE;
            msg = "Unstable";
        } else {
            state = GHCommitState.ERROR;
            msg = "Failed";
        }

        listener.getLogger().println("setting commit status on Github for " + repository.getUrl() + "/commit/" + sha1);
        repository.createCommitStatus(sha1, state, build.getAbsoluteUrl(), msg);
    }


    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "Set build status on GitHub commit";
        }
    }

}

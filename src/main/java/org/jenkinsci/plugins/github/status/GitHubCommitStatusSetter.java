package org.jenkinsci.plugins.github.status;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.plugins.github.common.CombineErrorHandler;
import org.jenkinsci.plugins.github.extension.status.GitHubCommitShaSource;
import org.jenkinsci.plugins.github.extension.status.GitHubReposSource;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusBackrefSource;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusContextSource;
import org.jenkinsci.plugins.github.extension.status.GitHubStatusResultSource;
import org.jenkinsci.plugins.github.extension.status.StatusErrorHandler;
import org.jenkinsci.plugins.github.status.sources.AnyDefinedRepositorySource;
import org.jenkinsci.plugins.github.status.sources.BuildDataRevisionShaSource;
import org.jenkinsci.plugins.github.status.sources.BuildRefBackrefSource;
import org.jenkinsci.plugins.github.status.sources.DefaultCommitContextSource;
import org.jenkinsci.plugins.github.status.sources.DefaultStatusResultSource;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.cloudbees.jenkins.Messages.GitHubCommitNotifier_SettingCommitStatus;

/**
 * Create commit state notifications on the commits based on the outcome of the build.
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public class GitHubCommitStatusSetter extends Notifier implements SimpleBuildStep {

    private GitHubCommitShaSource commitShaSource = new BuildDataRevisionShaSource();
    private GitHubReposSource reposSource = new AnyDefinedRepositorySource();
    private GitHubStatusContextSource contextSource = new DefaultCommitContextSource();
    private GitHubStatusResultSource statusResultSource = new DefaultStatusResultSource();
    private GitHubStatusBackrefSource statusBackrefSource = new BuildRefBackrefSource();
    private List<StatusErrorHandler> errorHandlers = new ArrayList<>();

    @DataBoundConstructor
    public GitHubCommitStatusSetter() {
    }

    @DataBoundSetter
    public void setCommitShaSource(GitHubCommitShaSource commitShaSource) {
        this.commitShaSource = commitShaSource;
    }

    @DataBoundSetter
    public void setReposSource(GitHubReposSource reposSource) {
        this.reposSource = reposSource;
    }

    @DataBoundSetter
    public void setContextSource(GitHubStatusContextSource contextSource) {
        this.contextSource = contextSource;
    }

    @DataBoundSetter
    public void setStatusResultSource(GitHubStatusResultSource statusResultSource) {
        this.statusResultSource = statusResultSource;
    }

    @DataBoundSetter
    public void setStatusBackrefSource(GitHubStatusBackrefSource statusBackrefSource) {
        this.statusBackrefSource = statusBackrefSource;
    }

    @DataBoundSetter
    public void setErrorHandlers(List<StatusErrorHandler> errorHandlers) {
        this.errorHandlers = errorHandlers;
    }

    /**
     * @return SHA provider
     */
    public GitHubCommitShaSource getCommitShaSource() {
        return commitShaSource;
    }

    /**
     * @return Repository list provider
     */
    public GitHubReposSource getReposSource() {
        return reposSource;
    }

    /**
     * @return Context provider
     */
    public GitHubStatusContextSource getContextSource() {
        return contextSource;
    }

    /**
     * @return state + msg provider
     */
    public GitHubStatusResultSource getStatusResultSource() {
        return statusResultSource;
    }

    /**
     * @return backref provider
     */
    public GitHubStatusBackrefSource getStatusBackrefSource() {
        return statusBackrefSource;
    }

    /**
     * @return error handlers
     */
    public List<StatusErrorHandler> getErrorHandlers() {
        return errorHandlers;
    }

    /**
     * Gets info from the providers and updates commit status
     */
    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) {
        try {
            String sha = getCommitShaSource().get(run, listener);
            List<GHRepository> repos = getReposSource().repos(run, listener);
            String contextName = getContextSource().context(run, listener);

            String backref = getStatusBackrefSource().get(run, listener);

            GitHubStatusResultSource.StatusResult result = getStatusResultSource().get(run, listener);

            String message = result.getMsg();
            GHCommitState state = result.getState();

            for (GHRepository repo : repos) {
                listener.getLogger().println(
                        GitHubCommitNotifier_SettingCommitStatus(repo.getHtmlUrl() + "/commit/" + sha)
                );

                repo.createCommitStatus(sha, state, backref, message, contextName);
            }

        } catch (Exception e) {
            CombineErrorHandler.errorHandling().withHandlers(getErrorHandlers()).handle(e, run, listener);
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public Object readResolve() {
        if (getStatusBackrefSource() == null) {
            setStatusBackrefSource(new BuildRefBackrefSource());
        }
        return this;
    }


    @Extension
    public static class GitHubCommitStatusSetterDescr extends BuildStepDescriptor<Publisher> {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Set status for GitHub commit [universal]";
        }

    }
}

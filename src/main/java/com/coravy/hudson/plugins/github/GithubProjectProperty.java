package com.coravy.hudson.plugins.github;

import com.cloudbees.jenkins.GitHubPushTrigger;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Stores the github related project properties.
 * <p>
 * - URL to the GitHub project
 * - Build status context name
 *
 * @author Stefan Saasen <stefan@coravy.com>
 */
public final class GithubProjectProperty extends JobProperty<Job<?, ?>> {

    /**
     * This will the URL to the project main branch.
     */
    private String projectUrl;

    /**
     * GitHub build status context name to use in commit status api
     * {@linkplain "https://developer.github.com/v3/repos/statuses/"}
     *
     * @see com.cloudbees.jenkins.GitHubCommitNotifier
     * @see com.cloudbees.jenkins.GitHubSetCommitStatusBuilder
     */
    private String statusContext;

    @DataBoundConstructor
    public GithubProjectProperty(String projectUrlStr) {
        this.projectUrl = new GithubUrl(projectUrlStr).baseUrl();
    }

    /**
     * Same as {@link #getProjectUrl}, but with a property name and type
     * which match those used in the {@link #GithubProjectProperty} constructor.
     * Should have been called {@code getProjectUrl} and that method called something else
     * (such as {@code getNormalizedProjectUrl}), but that cannot be done compatibly now.
     */
    public String getProjectUrlStr() {
        return projectUrl;
    }

    /**
     * @return the projectUrl
     */
    public GithubUrl getProjectUrl() {
        return new GithubUrl(projectUrl);
    }

    @CheckForNull
    public String getStatusContext() {
        return statusContext;
    }

    @DataBoundSetter
    public void setStatusContext(String statusContext) {
        this.statusContext = statusContext;
    }

    @Override
    public Collection<? extends Action> getJobActions(Job<?, ?> job) {
        if (null != projectUrl) {
            return Collections.singleton(new GithubLinkAction(this));
        }
        return Collections.emptyList();
    }

    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {
        /**
         * Used to hide property configuration under checkbox,
         * as of not each job is GitHub project
         */
        public static final String GITHUB_PROJECT_BLOCK_NAME = "githubProject";

        public boolean isApplicable(Class<? extends Job> jobType) {
            return ParameterizedJobMixIn.ParameterizedJob.class.isAssignableFrom(jobType);
        }

        public String getDisplayName() {
            return "GitHub project page";
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            GithubProjectProperty tpp = req.bindJSON(
                    GithubProjectProperty.class,
                    formData.getJSONObject(GITHUB_PROJECT_BLOCK_NAME)
            );

            if (tpp == null) {
                LOGGER.fine("Couldn't bind JSON");
                return null;
            }

            if (tpp.projectUrl == null) {
                LOGGER.fine("projectUrl not found, nullifying GithubProjectProperty");
                return null;
            }

            return tpp;
        }

    }

    private static final Logger LOGGER = Logger.getLogger(GitHubPushTrigger.class.getName());
}

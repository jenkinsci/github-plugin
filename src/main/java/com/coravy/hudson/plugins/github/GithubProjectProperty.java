package com.coravy.hudson.plugins.github;

import com.cloudbees.jenkins.GitHubPushTrigger;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import jenkins.model.ParameterizedJobMixIn;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.logging.Logger;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
    private String displayName;

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

    /**
     * @see #displayName
     * @since 1.14.1
     */
    @CheckForNull
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @since 1.14.1
     */
    @DataBoundSetter
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Extracts value of display name from given job, or just returns full name if field or prop is not defined
     *
     * @param job project which wants to get current context name to use in GH status API
     *
     * @return display name or full job name if field is not defined
     * @since 1.14.1
     */
    public static String displayNameFor(@Nonnull Job<?, ?> job) {
        GithubProjectProperty ghProp = job.getProperty(GithubProjectProperty.class);
        if (ghProp != null && isNotBlank(ghProp.getDisplayName())) {
            return ghProp.getDisplayName();
        }

        return job.getFullName();
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
        public JobProperty<?> newInstance(@Nonnull StaplerRequest req, JSONObject formData) throws FormException {
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

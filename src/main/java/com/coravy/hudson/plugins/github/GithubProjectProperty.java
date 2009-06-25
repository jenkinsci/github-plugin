package com.coravy.hudson.plugins.github;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Stores the github related project properties.
 * <p>
 * As of now this is only the URL to the github project.
 * 
 * @todo Should we store the GithubUrl instead of the String?
 * @author Stefan Saasen <stefan@coravy.com>
 */
public final class GithubProjectProperty extends
        JobProperty<AbstractProject<?, ?>> {

    /**
     * This will the URL to the project main branch.
     */
    private String projectUrl;

    @DataBoundConstructor
    public GithubProjectProperty(String projectUrl) {
        this.projectUrl = new GithubUrl(projectUrl).baseUrl();
    }

    /**
     * @return the projectUrl
     */
    public GithubUrl getProjectUrl() {
        return new GithubUrl(projectUrl);
    }

    @Override
    public Action getJobAction(AbstractProject<?, ?> job) {
        if (null != projectUrl) {
            return new GithubLinkAction(this);
        }
        return null;
    }
    /*
    @Override
    public JobPropertyDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    */
    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        public DescriptorImpl() {
            super(GithubProjectProperty.class);
            load();
        }

        public boolean isApplicable(Class<? extends Job> jobType) {
            return AbstractProject.class.isAssignableFrom(jobType);
        }

        public String getDisplayName() {
            return "Github project page";
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest req,
                JSONObject formData) throws FormException {
            GithubProjectProperty tpp = req.bindJSON(
                    GithubProjectProperty.class, formData);
            if (tpp.projectUrl == null) {
                tpp = null; // not configured
            }
            return tpp;
        }

    }
}

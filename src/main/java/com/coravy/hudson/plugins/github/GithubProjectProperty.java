package com.coravy.hudson.plugins.github;

import com.cloudbees.jenkins.GitHubPushTrigger;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Stores the github related project properties.
 * <p>
 * As of now this is only the URL to the github project.
 * 
 * @todo Should we store the GithubUrl instead of the String?
 * @author Stefan Saasen <stefan@coravy.com>
 */
public final class GithubProjectProperty extends
        JobProperty {

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
    public Collection<? extends Action> getJobActions(Job job) {
        if (null != projectUrl) {
            return Collections.singleton(new GithubLinkAction(this));
        }
        return Collections.emptyList();
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
            super();
            load();
        }

        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }

        public String getDisplayName() {
            return "GitHub project page";
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            GithubProjectProperty tpp = req.bindJSON(GithubProjectProperty.class, formData);

            if (tpp == null) {
                LOGGER.fine("Couldn't bind JSON");
                return null;
            }
            if (tpp.projectUrl == null) {
                tpp = null; // not configured
                LOGGER.fine("projectUrl not found, nullifying GithubProjectProperty");
            }
            return tpp;
        }

    }
    
    private static final Logger LOGGER = Logger.getLogger(GitHubPushTrigger.class.getName());
}

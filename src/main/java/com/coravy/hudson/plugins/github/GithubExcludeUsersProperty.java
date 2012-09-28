package com.coravy.hudson.plugins.github;

import hudson.Extension;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.AbstractProject;
import hudson.model.Job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
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
public final class GithubExcludeUsersProperty extends
  JobProperty<AbstractProject<?, ?>> {

  private static final Logger LOGGER = Logger.getLogger(GithubExcludeUsersProperty.class.getName());
  private static final String EXCLUDE_USERS_DELIMITER = ",";

  /**
   * This is the comma separate list of users
   */
  private String excludeUsers;

  @DataBoundConstructor
  public GithubExcludeUsersProperty(String excludeUsers) {
    this.excludeUsers = excludeUsers;
  }

  /**
   * @return the users to exclude
   * 
   *         TODO: Work out how to handle list properties correctly in jekins
   *         JobProperty
   */
  public List<String> getExcludeUsersList() {
    List<String> excludeUsersList = new ArrayList<String>();
    if (StringUtils.isNotBlank(excludeUsers)) {
      excludeUsersList = Arrays.asList(StringUtils.split(excludeUsers, EXCLUDE_USERS_DELIMITER));
    }
    return Collections.unmodifiableList(excludeUsersList);
  }

  public String getExcludeUsers() {
    return excludeUsers;
  }

  @Extension
  public static final class DescriptorImpl extends JobPropertyDescriptor {

    public DescriptorImpl() {
      super(GithubExcludeUsersProperty.class);
      load();
    }

    public boolean isApplicable(Class<? extends Job> jobType) {
      return AbstractProject.class.isAssignableFrom(jobType);
    }

    public String getDisplayName() {
      return "GitHub project page";
    }

    @Override
    public JobProperty<?> newInstance(StaplerRequest req,
      JSONObject formData) throws FormException {


      GithubExcludeUsersProperty tpp = req.bindJSON(
        GithubExcludeUsersProperty.class, formData);
      if (tpp.excludeUsers == null) {
        tpp = null; // not configured
      }
      return tpp;
    }
  }
}
package com.coravy.hudson.plugins.github;

import hudson.Extension;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.AbstractProject;
import hudson.model.Job;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Stores the names of users whose pushes will not casue a build to trigger
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
   *         TODO: Work out how to create this list once. Not sure about how
   *         marshalling is implemented
   */
  public List<String> getExcludeUsersList() {
    List<String> excludeUsersList = new ArrayList<String>();
    StringTokenizer st = new StringTokenizer(excludeUsers, EXCLUDE_USERS_DELIMITER);
    while (st.hasMoreTokens()) {
      excludeUsersList.add(st.nextToken().trim());
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
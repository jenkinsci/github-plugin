/*
 * $Id: GithubLinkAction.java 18781 2009-06-11 00:54:46Z juretta $ 
 */
package com.coravy.hudson.plugins.github;

import hudson.model.Action;

/**
 * Add the Github Logo/Icon to the sidebar.
 * 
 * @author Stefan Saasen <stefan@coravy.com>
 */
public final class GithubLinkAction implements Action {

    private final transient GithubProjectProperty projectProperty;

    public GithubLinkAction(GithubProjectProperty githubProjectProperty) {
        this.projectProperty = githubProjectProperty;
    }

    /*
     * (non-Javadoc)
     * @see hudson.model.Action#getDisplayName()
     */
    public String getDisplayName() {
        return "Github";
    }

    /*
     * (non-Javadoc)
     * @see hudson.model.Action#getIconFileName()
     */
    public String getIconFileName() {
        return "/plugin/github/github-octocat.png";
    }

    /*
     * (non-Javadoc)
     * @see hudson.model.Action#getUrlName()
     */
    public String getUrlName() {
        return projectProperty.getProjectUrl().baseUrl();
    }

}

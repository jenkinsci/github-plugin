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
        return "GitHub";
    }

    /*
     * (non-Javadoc)
     * @see hudson.model.Action#getIconFileName()
     */
    public String getIconFileName() {
        return "/plugin/github/logov3.png";
    }

    /*
     * (non-Javadoc)
     * @see hudson.model.Action#getUrlName()
     */
    public String getUrlName() {
        return projectProperty.getProjectUrl().baseUrl();
    }

}

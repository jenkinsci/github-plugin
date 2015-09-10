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

    @Override
    public String getDisplayName() {
        return "GitHub";
    }

    @Override
    public String getIconFileName() {
        return "/plugin/github/logov3.png";
    }

    @Override
    public String getUrlName() {
        return projectProperty.getProjectUrl().baseUrl();
    }

}

package com.coravy.hudson.plugins.github;

import hudson.Extension;
import hudson.model.Action;
import hudson.model.Job;
import jenkins.model.TransientActionFactory;
import org.jenkinsci.plugins.github.util.XSSApi;

import java.util.Collection;
import java.util.Collections;

/**
 * Add the GitHub Logo/Icon to the sidebar.
 *
 * @author <a href="mailto:stefan@coravy.com">Stefan Saasen</a>
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
        return "symbol-logo-github plugin-github";
    }

    @Override
    public String getUrlName() {
        return XSSApi.asValidHref(projectProperty.getProjectUrl().baseUrl());
    }

    @SuppressWarnings("rawtypes")
    @Extension
    public static class GithubLinkActionFactory extends TransientActionFactory<Job> {
        @Override
        public Class<Job> type() {
            return Job.class;
        }

        @Override
        public Collection<? extends Action> createFor(Job j) {
            GithubProjectProperty prop = ((Job<?, ?>) j).getProperty(GithubProjectProperty.class);

            if (prop == null) {
                return Collections.emptySet();
            } else {
                return Collections.singleton(new GithubLinkAction(prop));
            }
        }
    }
}

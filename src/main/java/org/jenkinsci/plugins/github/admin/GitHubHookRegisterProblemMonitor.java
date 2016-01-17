package org.jenkinsci.plugins.github.admin;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.google.common.collect.ImmutableMap;
import hudson.Extension;
import hudson.model.AdministrativeMonitor;
import hudson.model.ManagementLink;
import org.jenkinsci.plugins.github.Messages;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lanwen (Merkushev Kirill)
 */
@Extension
public class GitHubHookRegisterProblemMonitor extends AdministrativeMonitor {
    private Map<GitHubRepositoryName, Throwable> problems = new ConcurrentHashMap<>();

    public GitHubHookRegisterProblemMonitor() {
        super(GitHubHookRegisterProblemMonitor.class.getSimpleName());
    }

    public Map<GitHubRepositoryName, Throwable> getProblems() {
        return ImmutableMap.copyOf(problems);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void registerProblem(GitHubRepositoryName repo, Throwable throwable) {
        problems.put(repo, throwable);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void resolveProblem(GitHubRepositoryName repo) {
        problems.remove(repo);
    }

    public boolean isProblemWith(GitHubRepositoryName repo) {
        return problems.containsKey(repo);
    }

    @Override
    public String getDisplayName() {
        return Messages.hooks_problem_administrative_monitor_displayname();
    }

    @Override
    public boolean isActivated() {
        return !problems.isEmpty();
    }

    /**
     * Depending on whether the user said "yes" or "no", send him to the right place.
     */
    @RequirePOST
    public HttpResponse doAct(StaplerRequest req) throws IOException {
        if (req.hasParameter("no")) {
            disable(true);
            return HttpResponses.redirectViaContextPath("/manage");
        } else {
            return new HttpRedirect(".");
        }
    }

    public static GitHubHookRegisterProblemMonitor get() {
        return AdministrativeMonitor.all().get(GitHubHookRegisterProblemMonitor.class);
    }

    @Extension
    public static class GitHubHookRegisterProblemManagementLink extends ManagementLink {

        @Inject
        private GitHubHookRegisterProblemMonitor monitor;

        @Override
        public String getIconFileName() {
            return monitor.getProblems().isEmpty() ? null : "/plugin/github/img/ghlogo.svg";
        }

        @Override
        public String getUrlName() {
            return monitor.getUrl();
        }

        @Override
        public String getDescription() {
            return Messages.hooks_problem_administrative_monitor_description();
        }

        public String getDisplayName() {
            return Messages.hooks_problem_administrative_monitor_displayname();
        }
    }
}

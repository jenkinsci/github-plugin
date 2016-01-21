package org.jenkinsci.plugins.github.admin;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.google.common.collect.ImmutableMap;
import hudson.BulkChange;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.AdministrativeMonitor;
import hudson.model.ManagementLink;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import hudson.util.PersistedList;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.Messages;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lanwen (Merkushev Kirill)
 */
@Extension
public class GitHubHookRegisterProblemMonitor extends AdministrativeMonitor implements Saveable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubHookRegisterProblemMonitor.class);

    private transient Map<GitHubRepositoryName, Throwable> problems = new ConcurrentHashMap<>();
    private PersistedList<GitHubRepositoryName> ignored = new PersistedList<>(this);

    public GitHubHookRegisterProblemMonitor() {
        super(GitHubHookRegisterProblemMonitor.class.getSimpleName());
        load();
        ignored.setOwner(this);
    }

    public Map<GitHubRepositoryName, Throwable> getProblems() {
        return ImmutableMap.copyOf(problems);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void registerProblem(GitHubRepositoryName repo, Throwable throwable) {
        if (!ignored.contains(repo)) {
            problems.put(repo, throwable);
        } else {
            LOGGER.debug("Repo {} is in list of ignored, skip problem registering...");
        }
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void resolveProblem(GitHubRepositoryName repo) {
        problems.remove(repo);
    }

    public boolean isProblemWith(GitHubRepositoryName repo) {
        return problems.containsKey(repo);
    }

    public List<GitHubRepositoryName> getIgnored() {
        return ignored.toList();
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

    @RequirePOST
    public HttpResponse doIgnore(StaplerRequest req) throws IOException {
        GitHubRepositoryName repo = GitHubRepositoryName.create(req.getParameter("repo"));
        if (repo != null) {
            if (!ignored.contains(repo)) {
                ignored.add(repo);
            }
            resolveProblem(repo);
        }
        return new HttpRedirect(".");
    }

    @RequirePOST
    public HttpResponse doDisignore(StaplerRequest req) throws IOException {
        GitHubRepositoryName repo = GitHubRepositoryName.create(req.getParameter("repo"));
        if (repo != null) {
            ignored.remove(repo);
        }
        return new HttpRedirect(".");
    }

    /**
     * Save the settings to a file.
     */
    @Override
    public synchronized void save() {
        if (BulkChange.contains(this)) {
            return;
        }
        try {
            getConfigFile().write(this);
            SaveableListener.fireOnChange(this, getConfigFile());
        } catch (IOException e) {
            LOGGER.error("{}", e);
        }
    }

    public synchronized void load() {
        XmlFile file = getConfigFile();
        if (!file.exists()) {
            return;
        }
        try {
            file.unmarshal(this);
        } catch (IOException e) {
            LOGGER.warn("Failed to load {}", file, e);
        }
    }

    public final XmlFile getConfigFile() {
        return new XmlFile(new File(Jenkins.getInstance().getRootDir(), id + ".xml"));
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
            return monitor.getProblems().isEmpty() && monitor.ignored.isEmpty()
                    ? null
                    : "/plugin/github/img/ghlogo.svg";
        }

        @Override
        public String getUrlName() {
            return monitor.getUrl();
        }

        @Override
        public String getDescription() {
            return Messages.hooks_problem_administrative_monitor_description();
        }

        @Override
        public String getDisplayName() {
            return Messages.hooks_problem_administrative_monitor_displayname();
        }
    }
}

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

import edu.umd.cs.findbugs.annotations.NonNull;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * Administrative monitor to track problems of registering/removing hooks for GH.
 * Holds non-savable map of repo-&gt;message and persisted list of ignored projects.
 * Anyone can register new problem with {@link #registerProblem(GitHubRepositoryName, Throwable)} and check
 * repo for problems with {@link #isProblemWith(GitHubRepositoryName)}
 *
 * Has own page with table with problems and ignoring list in global management section. Link to this page
 * is visible if any problem or ignored repo is registered
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.17.0
 */
@Extension
public class GitHubHookRegisterProblemMonitor extends AdministrativeMonitor implements Saveable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubHookRegisterProblemMonitor.class);

    /**
     * Problems map. Cleared on Jenkins restarts
     */
    private transient Map<GitHubRepositoryName, String> problems = new ConcurrentHashMap<>();

    /**
     * Ignored list. Saved to file on any change. Reloaded after restart
     */
    private PersistedList<GitHubRepositoryName> ignored;

    public GitHubHookRegisterProblemMonitor() {
        super(GitHubHookRegisterProblemMonitor.class.getSimpleName());
        load();
        ignored = ignored == null ? new PersistedList<GitHubRepositoryName>(this) : ignored;
        ignored.setOwner(this);
    }

    /**
     * @return Immutable copy of map with repo-&gt;problem message content
     */
    public Map<GitHubRepositoryName, String> getProblems() {
        return ImmutableMap.copyOf(problems);
    }

    /**
     * Registers problems. For message {@link Throwable#getMessage()} will be used
     *
     * @param repo      full named GitHub repo, if null nothing will be done
     * @param throwable exception with message about problem, if null nothing will be done
     *
     * @see #registerProblem(GitHubRepositoryName, String)
     */
    public void registerProblem(GitHubRepositoryName repo, Throwable throwable) {
        if (throwable == null) {
            return;
        }
        registerProblem(repo, throwable.getMessage());
    }

    /**
     * Used by {@link #registerProblem(GitHubRepositoryName, Throwable)}
     *
     * @param repo    full named GitHub repo, if null nothing will be done
     * @param message message to show in the interface. Will be used default if blank
     */
    private void registerProblem(GitHubRepositoryName repo, String message) {
        if (repo == null) {
            return;
        }
        if (!ignored.contains(repo)) {
            problems.put(repo, defaultIfBlank(message, Messages.unknown_error()));
        } else {
            LOGGER.debug("Repo {} is ignored by monitor, skip this problem...", repo);
        }
    }

    /**
     * Removes repo from known problems map
     *
     * @param repo full named GitHub repo, if null nothing will be done
     */
    public void resolveProblem(GitHubRepositoryName repo) {
        if (repo == null) {
            return;
        }
        problems.remove(repo);
    }

    /**
     * Checks that repo is registered in this monitor
     *
     * @param repo full named GitHub repo
     *
     * @return true if repo is in the map
     */
    public boolean isProblemWith(GitHubRepositoryName repo) {
        return problems.containsKey(repo);
    }

    /**
     * @return immutable copy of list with ignored repos
     */
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
     * Depending on whether the user said "yes" or "no", send them to the right place.
     */
    @RequirePOST
    @RequireAdminRights
    public HttpResponse doAct(StaplerRequest req) throws IOException {
        if (req.hasParameter("no")) {
            disable(true);
            return HttpResponses.redirectViaContextPath("/manage");
        } else {
            return new HttpRedirect(".");
        }
    }

    /**
     * This web method requires POST, admin rights and nonnull repo.
     * Responds with redirect to monitor page
     *
     * @param repo to be ignored. Never null
     */
    @RequirePOST
    @ValidateRepoName
    @RequireAdminRights
    @RespondWithRedirect
    public void doIgnore(@NonNull @GHRepoName GitHubRepositoryName repo) {
        if (!ignored.contains(repo)) {
            ignored.add(repo);
        }
        resolveProblem(repo);
    }

    /**
     * This web method requires POST, admin rights and nonnull repo.
     * Responds with redirect to monitor page
     *
     * @param repo to be disignored. Never null
     */
    @RequirePOST
    @ValidateRepoName
    @RequireAdminRights
    @RespondWithRedirect
    public void doDisignore(@NonNull @GHRepoName GitHubRepositoryName repo) {
        ignored.remove(repo);
    }

    /**
     * Save the settings to a file. Called on each change of {@code ignored} list
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

    private synchronized void load() {
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

    private XmlFile getConfigFile() {
        return new XmlFile(new File(Jenkins.getInstance().getRootDir(), getClass().getName() + ".xml"));
    }

    /**
     * @return instance of administrative monitor to register/resolve/ignore/check hook problems
     */
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
                    : "symbol-logo-github plugin-ionicons-api";
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

        // TODO: Override `getCategory` instead using `Category.TROUBLESHOOTING` when minimum core version is 2.226+,
        // TODO: see https://github.com/jenkinsci/jenkins/commit/6de7e5fc7f6fb2e2e4cb342461788f97e3dfd8f6.
        protected String getCategoryName() {
            return "TROUBLESHOOTING";
        }
    }
}

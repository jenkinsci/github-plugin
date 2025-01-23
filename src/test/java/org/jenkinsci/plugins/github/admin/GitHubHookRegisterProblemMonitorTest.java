package org.jenkinsci.plugins.github.admin;

import com.cloudbees.jenkins.GitHubPushTrigger;
import com.cloudbees.jenkins.GitHubRepositoryName;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.plugins.git.GitSCM;
import jakarta.inject.Inject;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import org.jenkinsci.plugins.github.webhook.WebhookManager;
import org.jenkinsci.plugins.github.webhook.WebhookManagerTest;
import org.jenkinsci.plugins.github.webhook.subscriber.PingGHEventSubscriber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.jvnet.hudson.test.recipes.LocalData;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static com.cloudbees.jenkins.GitHubRepositoryName.create;
import static com.cloudbees.jenkins.GitHubWebHookFullTest.classpath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

/**
 * @author lanwen (Merkushev Kirill)
 */
@Issue("JENKINS-24690")
@WithJenkins
@ExtendWith(MockitoExtension.class)
class GitHubHookRegisterProblemMonitorTest {
    private static final GitHubRepositoryName REPO = new GitHubRepositoryName("host", "user", "repo");
    private static final String REPO_GIT_URI = "host/user/repo.git";
    private static final GitSCM REPO_GIT_SCM = new GitSCM("git://"+REPO_GIT_URI);

    private static final GitHubRepositoryName REPO_FROM_PING_PAYLOAD = create("https://github.com/lanwen/test");

    @Inject
    private GitHubHookRegisterProblemMonitor monitor;

    @Inject
    private GitHubHookRegisterProblemMonitor.GitHubHookRegisterProblemManagementLink link;

    @Inject
    private PingGHEventSubscriber pingSubscr;

    private JenkinsRule jRule;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private GitHub github;
    @Mock(strictness = Mock.Strictness.LENIENT)
    private GHRepository ghRepository;

    class GitHubServerConfigForTest extends GitHubServerConfig {
        public GitHubServerConfigForTest(String credentialsId) {
            super(credentialsId);
            this.setCachedClient(github);
        }
    }

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jRule = rule;
        jRule.getInstance().getInjector().injectMembers(this);
        GitHubServerConfig config = new GitHubServerConfigForTest("");
        config.setApiUrl("http://" + REPO_GIT_URI);
        GitHubPlugin.configuration().setConfigs(Arrays.asList(config));
        when(github.getRepository("user/repo")).thenReturn(ghRepository);
        when(ghRepository.hasAdminAccess()).thenReturn(true);
    }

    @Test
    void shouldRegisterProblem() throws Exception {
        monitor.registerProblem(REPO, new IOException());
        assertThat("should register problem", monitor.isProblemWith(REPO), is(true));
    }

    @Test
    void shouldResolveProblem() throws Exception {
        monitor.registerProblem(REPO, new IOException());
        monitor.resolveProblem(REPO);

        assertThat("should be no problem", monitor.isProblemWith(REPO), is(false));
    }

    @Test
    void shouldNotAddNullRepo() throws Exception {
        monitor.registerProblem(null, new IOException());
        assertThat("should be no problems", monitor.getProblems().keySet(), empty());
    }

    @Test
    void shouldNotAddNullExc() throws Exception {
        monitor.registerProblem(REPO, null);
        assertThat("should be no problems", monitor.getProblems().keySet(), empty());
    }

    @Test
    void shouldDoNothingOnNullResolve() throws Exception {
        monitor.registerProblem(REPO, new IOException());
        monitor.resolveProblem(null);

        assertThat("should not change anything", monitor.isProblemWith(REPO), is(true));
    }

    @Test
    void shouldBeDeactivatedByDefault() throws Exception {
        assertThat("should be deactivated", monitor.isActivated(), is(false));
    }

    @Test
    void shouldBeActivatedOnProblems() throws Exception {
        monitor.registerProblem(REPO, new IOException());
        assertThat("active on problems", monitor.isActivated(), is(true));
    }

    @Test
    void shouldResolveOnIgnoring() throws Exception {
        monitor.registerProblem(REPO, new IOException());
        monitor.doIgnore(REPO);

        assertThat("should be no problem", monitor.isProblemWith(REPO), is(false));
    }

    @Test
    void shouldNotRegisterNewOnIgnoring() throws Exception {
        monitor.doIgnore(REPO);
        monitor.registerProblem(REPO, new IOException());

        assertThat("should be no problem", monitor.isProblemWith(REPO), is(false));
    }

    @Test
    void shouldRemoveFromIgnoredOnDisignore() throws Exception {
        monitor.doIgnore(REPO);
        monitor.doDisignore(REPO);

        assertThat("should be no problem", monitor.getIgnored(), hasSize(0));
    }

    @Test
    void shouldNotAddRepoTwiceToIgnore() throws Exception {
        monitor.doIgnore(REPO);
        monitor.doIgnore(REPO);

        assertThat("twice ignored", monitor.getIgnored(), hasSize(1));
    }

    @Test
    @LocalData
    void shouldLoadIgnoredList() throws Exception {
        assertThat("loaded", monitor.getIgnored(), hasItem(equalTo(REPO)));
    }

    @Test
    void shouldReportAboutHookProblemOnRegister() throws IOException {
        FreeStyleProject job = jRule.createFreeStyleProject();
        job.addTrigger(new GitHubPushTrigger());
        job.setScm(REPO_GIT_SCM);

        when(github.getRepository("user/repo"))
                .thenThrow(new RuntimeException("shouldReportAboutHookProblemOnRegister"));
        WebhookManager.forHookUrl(WebhookManagerTest.HOOK_ENDPOINT)
                .registerFor((Item) job).run();

        assertThat("should reg problem", monitor.isProblemWith(REPO), is(true));
    }

    @Test
    void shouldNotReportAboutHookProblemOnRegister() throws IOException {
        FreeStyleProject job = jRule.createFreeStyleProject();
        job.addTrigger(new GitHubPushTrigger());
        job.setScm(REPO_GIT_SCM);

        WebhookManager.forHookUrl(WebhookManagerTest.HOOK_ENDPOINT)
                .registerFor((Item) job).run();

        assertThat("should reg problem", monitor.isProblemWith(REPO), is(false));
    }

    @Test
    void shouldReportAboutHookProblemOnUnregister() throws IOException {
        when(github.getRepository("user/repo"))
                .thenThrow(new RuntimeException("shouldReportAboutHookProblemOnUnregister"));
        WebhookManager.forHookUrl(WebhookManagerTest.HOOK_ENDPOINT)
                .unregisterFor(REPO, Collections.<GitHubRepositoryName>emptyList());

        assertThat("should reg problem", monitor.isProblemWith(REPO), is(true));
    }

    @Test
    void shouldNotReportAboutHookAuthProblemOnUnregister() {
        WebhookManager.forHookUrl(WebhookManagerTest.HOOK_ENDPOINT)
                .unregisterFor(REPO, Collections.<GitHubRepositoryName>emptyList());

        assertThat("should not reg problem", monitor.isProblemWith(REPO), is(false));
    }

    @Test
    void shouldResolveOnPingHook() {
        monitor.registerProblem(REPO_FROM_PING_PAYLOAD, new IOException());

        GHEventsSubscriber.processEvent(new GHSubscriberEvent("shouldResolveOnPingHook", GHEvent.PING, classpath("payloads/ping.json"))).apply(pingSubscr);

        assertThat("ping resolves problem", monitor.isProblemWith(REPO_FROM_PING_PAYLOAD), is(false));
    }

    @Test
    void shouldShowManagementLinkIfNonEmptyProblems() throws Exception {
        monitor.registerProblem(REPO, new IOException());
        assertThat("link on problems", link.getIconFileName(), notNullValue());
    }

    @Test
    void shouldShowManagementLinkIfNonEmptyIgnores() throws Exception {
        monitor.doIgnore(REPO);
        assertThat("link on ignores", link.getIconFileName(), notNullValue());
    }

    @Test
    void shouldShowManagementLinkIfBoth() throws Exception {
        monitor.registerProblem(REPO_FROM_PING_PAYLOAD, new IOException());
        monitor.doIgnore(REPO);
        assertThat("link on ignores", link.getIconFileName(), notNullValue());
    }

    @Test
    void shouldNotShowManagementLinkIfNoAny() throws Exception {
        assertThat("link on no any", link.getIconFileName(), nullValue());
    }
}

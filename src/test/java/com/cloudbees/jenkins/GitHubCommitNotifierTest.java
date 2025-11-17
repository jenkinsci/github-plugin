package com.cloudbees.jenkins;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import hudson.util.VersionNumber;
import jakarta.inject.Inject;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.test.GitHubMockExtension;
import org.jenkinsci.plugins.github.test.GitHubMockExtension.FixedGHRepoNameTestContributor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.cloudbees.jenkins.GitHubSetCommitStatusBuilderTest.SOME_SHA;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.jenkinsci.plugins.github.util.Messages.BuildDataHelper_NoBuildDataError;
import static org.jenkinsci.plugins.github.util.Messages.BuildDataHelper_NoLastRevisionError;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GitHubCommitNotifier}.
 *
 * @author <a href="mailto:o.v.nenashev@gmail.com">Oleg Nenashev</a>
 */
@WithJenkins
@ExtendWith(MockitoExtension.class)
public class GitHubCommitNotifierTest {

    @Mock(strictness = Mock.Strictness.LENIENT)
    public BuildData data;

    @Mock(strictness = Mock.Strictness.LENIENT)
    public Revision rev;

    @Inject
    public GitHubPluginConfig config;

    private JenkinsRule jRule;

    @RegisterExtension
    static GitHubMockExtension github = new GitHubMockExtension(WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().notifier(new Slf4jNotifier(true))))
            .stubUser()
            .stubRepo()
            .stubStatuses();


    @BeforeEach
    void before(JenkinsRule rule) throws Throwable {
        jRule = rule;
        jRule.getInstance().getInjector().injectMembers(this);

        when(data.getLastBuiltRevision()).thenReturn(rev);
        data.lastBuild = new hudson.plugins.git.util.Build(rev, rev, 0, Result.SUCCESS);
        when(rev.getSha1()).thenReturn(ObjectId.fromString(SOME_SHA));
    }

    @Test
    @Issue("JENKINS-23641")
    void testNoBuildData() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject("23641_noBuildData");
        prj.getPublishersList().add(new GitHubCommitNotifier());
        Build b = prj.scheduleBuild2(0).get();
        jRule.assertBuildStatus(Result.FAILURE, b);
        jRule.assertLogContains(BuildDataHelper_NoBuildDataError(), b);
    }

    @Test
    @Issue("JENKINS-23641")
    void testNoBuildRevision() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject();
        prj.setScm(new GitSCM("http://non.existent.git.repo.nowhere/repo.git"));
        prj.getPublishersList().add(new GitHubCommitNotifier());
        //Git plugin 2.4.1 + does not include BuildData if checkout fails, so we add it if needed
        Build b = safelyGenerateBuild(prj);
        jRule.assertBuildStatus(Result.FAILURE, b);
        jRule.assertLogContains(BuildDataHelper_NoLastRevisionError(), b);
    }

    @Test
    @Issue("JENKINS-25312")
    void testMarkUnstableOnCommitNotifierFailure() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject();
        prj.getPublishersList().add(new GitHubCommitNotifier(Result.UNSTABLE.toString()));
        Build b = prj.scheduleBuild2(0).get();
        jRule.assertBuildStatus(Result.UNSTABLE, b);
    }

    @Test
    @Issue("JENKINS-25312")
    void testMarkSuccessOnCommitNotifierFailure() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject();
        prj.getPublishersList().add(new GitHubCommitNotifier(Result.SUCCESS.toString()));
        Build b = prj.scheduleBuild2(0).get();
        jRule.assertBuildStatus(Result.SUCCESS, b);
    }

    @Test
    void shouldWriteStatusOnGH() throws Exception {
        config.getConfigs().add(github.serverConfig());
        FreeStyleProject prj = jRule.createFreeStyleProject();

        prj.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                build.addAction(data);
                return true;
            }
        });

        prj.getPublishersList().add(new GitHubCommitNotifier(Result.SUCCESS.toString()));

        prj.scheduleBuild2(0).get();

        github.verify(1, postRequestedFor(urlPathMatching(".*/" + SOME_SHA)));
    }

    private Build safelyGenerateBuild(FreeStyleProject prj) throws InterruptedException, java.util.concurrent.ExecutionException {
        Build b;
        if (jRule.getPluginManager().getPlugin("git").getVersionNumber().isNewerThan(new VersionNumber("2.4.0"))) {
            b = prj.scheduleBuild2(0, new Cause.UserIdCause(), new BuildData()).get();
        } else {
            b = prj.scheduleBuild2(0).get();
        }
        return b;
    }

    @TestExtension
    public static final FixedGHRepoNameTestContributor CONTRIBUTOR = new FixedGHRepoNameTestContributor();

}

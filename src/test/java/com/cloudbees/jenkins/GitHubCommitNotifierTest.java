package com.cloudbees.jenkins;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
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
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.test.GHMockRule;
import org.jenkinsci.plugins.github.test.GHMockRule.FixedGHRepoNameTestContributor;
import org.jenkinsci.plugins.github.test.InjectJenkinsMembersRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.TestExtension;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Inject;

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
 * @author Oleg Nenashev <o.v.nenashev@gmail.com>
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubCommitNotifierTest {

    @Mock
    public BuildData data;

    @Mock
    public Revision rev;

    @Inject
    public GitHubPluginConfig config;

    public JenkinsRule jRule = new JenkinsRule();

    @Rule
    public RuleChain chain = RuleChain.outerRule(jRule).around(new InjectJenkinsMembersRule(jRule, this));

    @Rule
    public GHMockRule github = new GHMockRule(
            new WireMockRule(
                    wireMockConfig().dynamicPort().notifier(new Slf4jNotifier(true))
            ))
            .stubUser()
            .stubRepo()
            .stubStatuses();


    @Rule
    public ExternalResource prep = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            when(data.getLastBuiltRevision()).thenReturn(rev);
            data.lastBuild = new hudson.plugins.git.util.Build(rev, rev, 0, Result.SUCCESS);
            when(rev.getSha1()).thenReturn(ObjectId.fromString(SOME_SHA));
        }
    };

    @Test
    @Issue("JENKINS-23641")
    public void testNoBuildData() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject("23641_noBuildData");
        prj.getPublishersList().add(new GitHubCommitNotifier());
        Build b = prj.scheduleBuild2(0).get();
        jRule.assertBuildStatus(Result.FAILURE, b);
        jRule.assertLogContains(BuildDataHelper_NoBuildDataError(), b);
    }

    @Test
    @Issue("JENKINS-23641")
    public void testNoBuildRevision() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject();
        prj.setScm(new GitSCM("http://non.existent.git.repo.nowhere/repo.git"));
        prj.getPublishersList().add(new GitHubCommitNotifier());
        Build b = null;
        b = safelyGenerateBuild(prj);
        jRule.assertBuildStatus(Result.FAILURE, b);
        jRule.assertLogContains(BuildDataHelper_NoLastRevisionError(), b);
    }

    @Test
    @Issue("JENKINS-25312")
    public void testMarkUnstableOnCommitNotifierFailure() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject();
        prj.getPublishersList().add(new GitHubCommitNotifier(Result.UNSTABLE.toString()));
        Build b = prj.scheduleBuild2(0).get();
        jRule.assertBuildStatus(Result.UNSTABLE, b);
    }

    @Test
    @Issue("JENKINS-25312")
    public void testMarkSuccessOnCommitNotifierFailure() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject();
        prj.getPublishersList().add(new GitHubCommitNotifier(Result.SUCCESS.toString()));
        Build b = prj.scheduleBuild2(0).get();
        jRule.assertBuildStatus(Result.SUCCESS, b);
    }

    @Test
    public void shouldWriteStatusOnGH() throws Exception {
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

        github.service().verify(1, postRequestedFor(urlPathMatching(".*/" + SOME_SHA)));
    }

    private Build safelyGenerateBuild(FreeStyleProject prj) throws InterruptedException, java.util.concurrent.ExecutionException {
        //Git plugin 2.4.1 + does not include BuildData if checkout fails, so we add it if needed
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

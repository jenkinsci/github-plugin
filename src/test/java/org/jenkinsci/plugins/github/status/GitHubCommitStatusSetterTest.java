package org.jenkinsci.plugins.github.status;

import com.cloudbees.jenkins.GitHubSetCommitStatusBuilder;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.extension.status.StatusErrorHandler;
import org.jenkinsci.plugins.github.status.err.ChangingBuildStatusErrorHandler;
import org.jenkinsci.plugins.github.status.sources.AnyDefinedRepositorySource;
import org.jenkinsci.plugins.github.status.sources.BuildDataRevisionShaSource;
import org.jenkinsci.plugins.github.status.sources.DefaultCommitContextSource;
import org.jenkinsci.plugins.github.status.sources.DefaultStatusResultSource;
import org.jenkinsci.plugins.github.test.GHMockRule;
import org.jenkinsci.plugins.github.test.GHMockRule.FixedGHRepoNameTestContributor;
import org.jenkinsci.plugins.github.test.InjectJenkinsMembersRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.TestExtension;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.inject.Inject;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GitHubSetCommitStatusBuilder}.
 *
 * @author <a href="mailto:o.v.nenashev@gmail.com">Oleg Nenashev</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubCommitStatusSetterTest {

    public static final String SOME_SHA = StringUtils.repeat("f", 40);

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

    @Before
    public void before() throws Throwable {
            when(data.getLastBuiltRevision()).thenReturn(rev);
            data.lastBuild = new hudson.plugins.git.util.Build(rev, rev, 0, Result.SUCCESS);
            when(rev.getSha1()).thenReturn(ObjectId.fromString(SOME_SHA));
    }


    @Test
    public void shouldSetGHCommitStatus() throws Exception {
        config.getConfigs().add(github.serverConfig());
        FreeStyleProject prj = jRule.createFreeStyleProject();

        GitHubCommitStatusSetter statusSetter = new GitHubCommitStatusSetter();
        statusSetter.setCommitShaSource(new BuildDataRevisionShaSource());
        statusSetter.setContextSource(new DefaultCommitContextSource());
        statusSetter.setReposSource(new AnyDefinedRepositorySource());
        statusSetter.setStatusResultSource(new DefaultStatusResultSource());


        prj.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                build.addAction(data);
                return true;
            }
        });

        prj.getPublishersList().add(statusSetter);
        prj.scheduleBuild2(0).get();

        github.service().verify(1, postRequestedFor(urlPathMatching(".*/" + SOME_SHA)));
    }

    @Test
    public void shouldHandleError() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject();

        GitHubCommitStatusSetter statusSetter = new GitHubCommitStatusSetter();
        statusSetter.setCommitShaSource(new BuildDataRevisionShaSource());
        statusSetter.setErrorHandlers(Collections.<StatusErrorHandler>singletonList(
                new ChangingBuildStatusErrorHandler(Result.UNSTABLE.toString())
        ));
        statusSetter.setReposSource(new AnyDefinedRepositorySource());
        statusSetter.setStatusResultSource(new DefaultStatusResultSource());

        prj.getPublishersList().add(statusSetter);
        FreeStyleBuild build = prj.scheduleBuild2(0).get();
        jRule.assertBuildStatus(Result.UNSTABLE, build);
    }

    @TestExtension
    public static final FixedGHRepoNameTestContributor CONTRIBUTOR = new FixedGHRepoNameTestContributor();
}

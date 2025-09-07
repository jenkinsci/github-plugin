package org.jenkinsci.plugins.github.status;

import com.cloudbees.jenkins.GitHubSetCommitStatusBuilder;
import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.extension.status.StatusErrorHandler;
import org.jenkinsci.plugins.github.status.err.ChangingBuildStatusErrorHandler;
import org.jenkinsci.plugins.github.status.sources.AnyDefinedRepositorySource;
import org.jenkinsci.plugins.github.status.sources.BuildDataRevisionShaSource;
import org.jenkinsci.plugins.github.status.sources.DefaultCommitContextSource;
import org.jenkinsci.plugins.github.status.sources.DefaultStatusResultSource;
import org.jenkinsci.plugins.github.test.GitHubMockExtension;
import org.jenkinsci.plugins.github.test.GitHubMockExtension.FixedGHRepoNameTestContributor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
@WithJenkins
@ExtendWith(MockitoExtension.class)
public class GitHubCommitStatusSetterTest {

    public static final String SOME_SHA = StringUtils.repeat("f", 40);

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
    void shouldSetGHCommitStatus() throws Exception {
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

        github.verify(1, postRequestedFor(urlPathMatching(".*/" + SOME_SHA)));
    }

    @Test
    void shouldHandleError() throws Exception {
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

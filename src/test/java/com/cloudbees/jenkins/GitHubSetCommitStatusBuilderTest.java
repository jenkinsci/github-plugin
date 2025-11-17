package com.cloudbees.jenkins;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import hudson.tasks.Builder;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
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
import org.jvnet.hudson.test.recipes.LocalData;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GitHubSetCommitStatusBuilder}.
 *
 * @author <a href="mailto:o.v.nenashev@gmail.com">Oleg Nenashev</a>
 */
@WithJenkins
@ExtendWith(MockitoExtension.class)
public class GitHubSetCommitStatusBuilderTest {

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
    @Issue("JENKINS-23641")
    void shouldIgnoreIfNoBuildData() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject("23641_noBuildData");
        prj.getBuildersList().add(new GitHubSetCommitStatusBuilder());
        Build b = prj.scheduleBuild2(0).get();
        jRule.assertBuildStatus(Result.SUCCESS, b);
    }

    @Test
    @LocalData
    @Issue("JENKINS-32132")
    void shouldLoadNullStatusMessage() throws Exception {
        config.getConfigs().add(github.serverConfig());
        FreeStyleProject prj = jRule.getInstance().getItemByFullName("step", FreeStyleProject.class);

        List<Builder> builders = newArrayList(prj.getBuildersList().toList());
        builders.add(0, new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                build.addAction(data);
                return true;
            }
        });

        prj.getBuildersList().replaceBy(builders);
        prj.scheduleBuild2(0).get();

        github.verify(1, postRequestedFor(urlPathMatching(".*/" + SOME_SHA)));
    }

    @TestExtension
    public static final FixedGHRepoNameTestContributor CONTRIBUTOR = new FixedGHRepoNameTestContributor();
}

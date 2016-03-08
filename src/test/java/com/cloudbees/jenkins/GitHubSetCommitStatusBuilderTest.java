package com.cloudbees.jenkins;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import hudson.tasks.Builder;
import org.apache.commons.lang3.StringUtils;
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
import org.jvnet.hudson.test.recipes.LocalData;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Inject;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GitHubSetCommitStatusBuilder}.
 *
 * @author Oleg Nenashev <o.v.nenashev@gmail.com>
 */
@RunWith(MockitoJUnitRunner.class)
public class GitHubSetCommitStatusBuilderTest {

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

    @Rule
    public ExternalResource prep = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            when(data.getLastBuiltRevision()).thenReturn(rev);
            data.lastBuild = new hudson.plugins.git.util.Build(rev,rev,0,Result.SUCCESS);
            when(rev.getSha1()).thenReturn(ObjectId.fromString(SOME_SHA));
        }
    };

    @Test
    @Issue("JENKINS-23641")
    public void testNoBuildData() throws Exception {
        FreeStyleProject prj = jRule.createFreeStyleProject("23641_noBuildData");
        prj.getBuildersList().add(new GitHubSetCommitStatusBuilder());
        Build b = prj.scheduleBuild2(0).get();
        jRule.assertBuildStatus(Result.FAILURE, b);
        jRule.assertLogContains(org.jenkinsci.plugins.github.util.Messages.BuildDataHelper_NoBuildDataError(), b);
    }

    @Test
    @LocalData
    @Issue("JENKINS-32132")
    public void shouldLoadNullStatusMessage() throws Exception {
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

        github.service().verify(1, postRequestedFor(urlPathMatching(".*/" + SOME_SHA)));
    }

    @TestExtension
    public static final FixedGHRepoNameTestContributor CONTRIBUTOR = new FixedGHRepoNameTestContributor();
}

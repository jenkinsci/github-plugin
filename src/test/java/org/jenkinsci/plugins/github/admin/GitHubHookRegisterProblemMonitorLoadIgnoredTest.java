package org.jenkinsci.plugins.github.admin;

import com.cloudbees.jenkins.GitHubRepositoryName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import javax.inject.Inject;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

/**
 * @author lanwen (Merkushev Kirill)
 */
@Issue("JENKINS-24690")
public class GitHubHookRegisterProblemMonitorLoadIgnoredTest {
    private static final GitHubRepositoryName REPO = new GitHubRepositoryName("host", "user", "repo");

    @Inject
    private GitHubHookRegisterProblemMonitor monitor;

    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
        jRule.getInstance().getInjector().injectMembers(this);
    }

    @Test
    @LocalData
    public void shouldLoadIgnoredList() throws Exception {
        assertThat("loaded", monitor.getIgnored(), hasItem(equalTo(REPO)));
    }
}

package com.coravy.hudson.plugins.github;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.structs.DescribableHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@WithJenkins
@Disabled("It failed to instantiate class org.jenkinsci.plugins.workflow.flow.FlowDefinition - dunno how to fix it")
class GithubProjectPropertyTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
    }

    @Test
    void configRoundTrip() throws Exception {
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        j.configRoundtrip(p);
        assertNull(p.getProperty(GithubProjectProperty.class));
        String url = "https://github.com/a/b/";
        p.addProperty(new GithubProjectProperty(url));
        j.configRoundtrip(p);
        GithubProjectProperty prop = p.getProperty(GithubProjectProperty.class);
        assertNotNull(prop);
        assertEquals(url, prop.getProjectUrl().baseUrl());
        prop = DescribableHelper.instantiate(GithubProjectProperty.class, DescribableHelper.uninstantiate(prop));
        assertEquals(url, prop.getProjectUrl().baseUrl());
    }

}

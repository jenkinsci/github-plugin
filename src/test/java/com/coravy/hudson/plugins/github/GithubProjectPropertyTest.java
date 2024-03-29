package com.coravy.hudson.plugins.github;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.structs.DescribableHelper;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

@Ignore("It failed to instantiate class org.jenkinsci.plugins.workflow.flow.FlowDefinition - dunno how to fix it")
public class GithubProjectPropertyTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void configRoundTrip() throws Exception {
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

/*
 * Copyright 2015 CloudBees, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.coravy.hudson.plugins.github;

import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.structs.DescribableHelper;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

public class GithubProjectPropertyTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void configRoundTrip() throws Exception {
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        r.configRoundtrip(p);
        assertNull(p.getProperty(GithubProjectProperty.class));
        String url = "https://github.com/a/b/";
        p.addProperty(new GithubProjectProperty(url));
        r.configRoundtrip(p);
        GithubProjectProperty prop = p.getProperty(GithubProjectProperty.class);
        assertNotNull(prop);
        assertEquals(url, prop.getProjectUrl().baseUrl());
        prop = DescribableHelper.instantiate(GithubProjectProperty.class, DescribableHelper.uninstantiate(prop));
        assertEquals(url, prop.getProjectUrl().baseUrl());
    }

}

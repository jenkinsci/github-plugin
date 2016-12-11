package org.jenkinsci.plugins.github.common;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class ExpandableMessageTest {

    public static final String ENV_VAR_JOB_NAME = "JOB_NAME";
    public static final String CUSTOM_BUILD_PARAM = "FOO";
    public static final String CUSTOM_PARAM_VAL = "BAR";
    public static final String MSG_FORMAT = "%s - %s - %s";
    public static final String DEFAULT_TOKEN_TEMPLATE = "${ENV, var=\"%s\"}";

    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    @Test
    public void shouldExpandEnvAndBuildVars() throws Exception {
        MessageExpander expander = new MessageExpander(new ExpandableMessage(
                format(MSG_FORMAT,
                        asVar(ENV_VAR_JOB_NAME),
                        asVar(CUSTOM_BUILD_PARAM),
                        asTokenVar(ENV_VAR_JOB_NAME)
                )
        ));

        FreeStyleProject job = jRule.createFreeStyleProject();
        job.getBuildersList().add(expander);

        job.scheduleBuild2(0, new ParametersAction(new StringParameterValue(CUSTOM_BUILD_PARAM, CUSTOM_PARAM_VAL)))
                .get(5, TimeUnit.SECONDS);

        assertThat("job name - var param - template", expander.getResult(),
                startsWith(format(MSG_FORMAT, job.getFullName(), CUSTOM_PARAM_VAL, job.getFullName())));
    }

    public static String asVar(String name) {
        return format("${%s}", name);
    }

    public static String asTokenVar(String name) {
        return format(DEFAULT_TOKEN_TEMPLATE, name);
    }

    private static class MessageExpander extends TestBuilder {
        private ExpandableMessage message;
        private String result;

        public MessageExpander(ExpandableMessage message) {
            this.message = message;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            result = message.expandAll(build, listener);
            return true;
        }

        public String getResult() {
            return result;
        }
    }
}

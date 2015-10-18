package org.jenkinsci.plugins.github.common;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.plugins.emailext.plugins.ContentBuilder;
import org.jenkinsci.plugins.github.test.WithoutPlugins;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class ExpandableMessageTest {

    public static final String ENV_VAR_JOB_NAME = "JOB_NAME";
    public static final String CUSTOM_BUILD_PARAM = "FOO";
    public static final String CUSTOM_PARAM_VAL = "BAR";
    public static final String MSG_FORMAT = "%s - %s - %s";
    public static final String DEFAULT_EMAIL_EXT_TEMPLATE = "${SCRIPT, template=\"groovy-text.template\"}";

    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    @Test
    public void shouldNotChangeSignatureOfGettingPrivateMacro() throws Exception {
        assertThat("should be static method of email-ext plugin", ContentBuilder.getPrivateMacros(), notNullValue());
    }

    @Test
    public void shouldExpandEnvAndBuildVars() throws Exception {
        MessageExpander expander = new MessageExpander(new ExpandableMessage(
                format(MSG_FORMAT,
                        asVar(ENV_VAR_JOB_NAME),
                        asVar(CUSTOM_BUILD_PARAM),
                        DEFAULT_EMAIL_EXT_TEMPLATE
                )
        ));

        FreeStyleProject job = jRule.createFreeStyleProject();
        job.getBuildersList().add(expander);

        job.scheduleBuild2(0, new ParametersAction(new StringParameterValue(CUSTOM_BUILD_PARAM, CUSTOM_PARAM_VAL)))
                .get(5, TimeUnit.SECONDS);

        assertThat("job name - var param - template", expander.getResult(),
                startsWith(format(MSG_FORMAT, job.getFullName(), CUSTOM_PARAM_VAL, "GENERAL INFO\n\nBUILD")));
    }

    @Test
    @WithoutPlugins
    public void shouldNotFailWithDisabledEmailExt() throws Exception {
        MessageExpander expander = new MessageExpander(new ExpandableMessage(DEFAULT_EMAIL_EXT_TEMPLATE));

        FreeStyleProject job = jRule.createFreeStyleProject();
        job.getBuildersList().add(expander);
        jRule.buildAndAssertSuccess(job);

        assertThat("should not change", expander.getResult(), is(DEFAULT_EMAIL_EXT_TEMPLATE));
    }


    public static String asVar(String name) {
        return format("${%s}", name);
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

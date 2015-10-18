package org.jenkinsci.plugins.github.common;

import com.cloudbees.jenkins.GitHubWebHook;
import hudson.Extension;
import hudson.Plugin;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.Messages;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * Represents a message that can contain token macros.
 *
 * uses https://wiki.jenkins-ci.org/display/JENKINS/Token+Macro+Plugin to expand vars
 *
 * @author Kanstantsin Shautsou
 * @author Alina Karpovich
 * @since 1.14.1
 */
public class ExpandableMessage extends AbstractDescribableImpl<ExpandableMessage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpandableMessage.class);

    /**
     * https://wiki.jenkins-ci.org/display/JENKINS/Email-ext+plugin
     */
    public static final String EMAIL_EXT_PLUGIN_ID = "email-ext";

    private final String content;

    @DataBoundConstructor
    public ExpandableMessage(String content) {
        this.content = content;
    }

    /**
     * Expands all env vars. In case of AbstractBuild also expands token macro and build vars
     *
     * @param run      build context
     * @param listener usually used to log something to console while building env vars
     *
     * @return string with expanded vars and tokens
     */
    public String expandAll(Run<?, ?> run, TaskListener listener) throws IOException, InterruptedException {
        if (run instanceof AbstractBuild) {
            try {
                return TokenMacro.expandAll((AbstractBuild) run, listener, content, false, loadPrivateTokens());
            } catch (MacroEvaluationException e) {
                LOGGER.error("Can't process token content {} in {} ({})",
                        content, run.getParent().getFullName(), e.getMessage());
                LOGGER.trace(e.getMessage(), e);
                return content;
            }
        } else {
            // fallback to env vars only because of token-macro allow only AbstractBuild in 1.11
            return run.getEnvironment(listener).expand(trimToEmpty(content));
        }
    }

    public String getContent() {
        return content;
    }

    /**
     * Macro list like groovy template (${SCRIPT, template=''})
     * More info about code
     * https://wiki.jenkins-ci.org/display/JENKINS/Tips+for+optional+dependencies
     *
     * @return private macro list from email-ext or empty list if no such plugin installed
     */
    @SuppressWarnings("unchecked")
    private static List loadPrivateTokens() {
        Plugin emailExt = plugin(EMAIL_EXT_PLUGIN_ID);

        if (emailExt != null) {
            try {
                return new ArrayList((Collection) find(emailExt, "hudson.plugins.emailext.plugins.ContentBuilder")
                        .getDeclaredMethod("getPrivateMacros")
                        .invoke(null));
            } catch (NoSuchMethodException | ClassNotFoundException e) {
                LOGGER.error("Can't load class", e);
            } catch (InvocationTargetException | IllegalAccessException e) {
                LOGGER.error("Can't get private macro list from {}", EMAIL_EXT_PLUGIN_ID, e);
            }
        }

        return Collections.emptyList();
    }

    private static Plugin plugin(String id) {
        return GitHubWebHook.getJenkinsInstance().getPlugin(id);
    }

    private static Class<?> find(Plugin plugin, String className) throws ClassNotFoundException {
        return plugin.getWrapper().classLoader.loadClass(className);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ExpandableMessage> {
        @Override
        public String getDisplayName() {
            return Messages.common_expandable_message_title();
        }
    }
}

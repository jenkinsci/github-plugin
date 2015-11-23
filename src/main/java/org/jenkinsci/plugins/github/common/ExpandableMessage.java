package org.jenkinsci.plugins.github.common;

import hudson.Extension;
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
import java.util.Collections;

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
                return TokenMacro.expandAll(
                        (AbstractBuild) run,
                        listener,
                        content,
                        false,
                        Collections.<TokenMacro>emptyList()
                );
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

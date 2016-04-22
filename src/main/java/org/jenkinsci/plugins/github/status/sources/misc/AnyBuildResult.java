package org.jenkinsci.plugins.github.status.sources.misc;

import hudson.Extension;
import hudson.model.Run;
import org.jenkinsci.plugins.github.extension.status.misc.ConditionalResult;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class AnyBuildResult extends ConditionalResult {

    @DataBoundConstructor
    public AnyBuildResult() {
    }

    @Override
    public boolean matches(@Nonnull Run<?, ?> run) {
        return true;
    }

    @Extension
    public static class AnyBuildResultDescriptor extends ConditionalResultDescriptor {
        @Override
        public String getDisplayName() {
            return "Result ANY";
        }
    }
}

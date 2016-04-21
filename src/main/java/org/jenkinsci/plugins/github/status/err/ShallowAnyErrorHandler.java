package org.jenkinsci.plugins.github.status.err;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.extension.status.StatusErrorHandler;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class ShallowAnyErrorHandler extends StatusErrorHandler {

    @DataBoundConstructor
    public ShallowAnyErrorHandler() {
    }

    @Override
    public boolean handle(Exception e, @Nonnull Run<?, ?> run, @Nonnull TaskListener listener) {
        listener.error("[GitHub Commit Status Setter] Failed to update commit status on GitHub. " +
                "Ignoring exception [%s]", e.getMessage());
        return true;
    }

    @Extension
    public static class ShallowAnyErrorHandlerDescriptor extends Descriptor<StatusErrorHandler> {
        @Override
        public String getDisplayName() {
            return "Just ignore any errors";
        }
    }
}

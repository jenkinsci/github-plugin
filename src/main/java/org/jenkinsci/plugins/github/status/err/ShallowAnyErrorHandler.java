package org.jenkinsci.plugins.github.status.err;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.extension.status.StatusErrorHandler;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/**
 * Just logs message to the build console and do nothing after it
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public class ShallowAnyErrorHandler extends StatusErrorHandler {

    @DataBoundConstructor
    public ShallowAnyErrorHandler() {
    }

    /**
     * @return true as of its terminating handler
     */
    @Override
    public boolean handle(Exception e, @Nonnull Run<?, ?> run, @Nonnull TaskListener listener) {
        listener.error("[GitHub Commit Status Setter] Failed to update commit state on GitHub. " +
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

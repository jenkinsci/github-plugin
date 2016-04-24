package org.jenkinsci.plugins.github.status.err;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.github.extension.status.StatusErrorHandler;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

import static hudson.model.Result.FAILURE;
import static hudson.model.Result.UNSTABLE;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * Can change build status in case of errors
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public class ChangingBuildStatusErrorHandler extends StatusErrorHandler {

    private String result;

    @DataBoundConstructor
    public ChangingBuildStatusErrorHandler(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    /**
     * Logs error to build console and changes build result
     *
     * @return true as of it terminating handler
     */
    @Override
    public boolean handle(Exception e, @Nonnull Run<?, ?> run, @Nonnull TaskListener listener) {
        Result toSet = Result.fromString(trimToEmpty(result));

        listener.error("[GitHub Commit Status Setter] - %s, setting build result to %s", e.getMessage(), toSet);

        run.setResult(toSet);
        return true;
    }

    @Extension
    public static class ChangingBuildStatusErrorHandlerDescriptor extends Descriptor<StatusErrorHandler> {
        
        private static final Result[] SUPPORTED_RESULTS = {
                FAILURE,
                UNSTABLE,
        };

        @Override
        public String getDisplayName() {
            return "Change build status";
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillResultItems() {
            ListBoxModel items = new ListBoxModel();
            for (Result supported : SUPPORTED_RESULTS) {
                items.add(supported.toString());
            }
            return items;
        }
    }
}

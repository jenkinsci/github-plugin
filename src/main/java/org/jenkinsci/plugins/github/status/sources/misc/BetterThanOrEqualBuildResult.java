package org.jenkinsci.plugins.github.status.sources.misc;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import hudson.util.ListBoxModel;
import org.jenkinsci.plugins.github.extension.status.misc.ConditionalResult;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;

import static hudson.model.Result.FAILURE;
import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;
import static hudson.model.Result.fromString;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class BetterThanOrEqualBuildResult extends ConditionalResult {

    private String result;

    @DataBoundConstructor
    public BetterThanOrEqualBuildResult() {
    }

    @DataBoundSetter
    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    @Override
    public boolean matches(@Nonnull Run<?, ?> run) {
        return defaultIfNull(run.getResult(), Result.NOT_BUILT).isBetterOrEqualTo(fromString(trimToEmpty(result)));
    }

    @Extension
    public static class BetterThanOrEqualBuildResultDescriptor extends ConditionalResultDescriptor {
        private static final Result[] SUPPORTED_RESULTS = {
                SUCCESS,
                UNSTABLE,
                FAILURE,
        };

        @Override
        public String getDisplayName() {
            return "Result better than or equal to";
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

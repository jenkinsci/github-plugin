package org.jenkinsci.plugins.github.status.sources.misc;

import hudson.Extension;
import hudson.model.Run;
import org.jenkinsci.plugins.github.extension.status.misc.ConditionalResult;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

/**
 * Allows to set state in any case
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public class AnyBuildResult extends ConditionalResult {

    @DataBoundConstructor
    public AnyBuildResult() {
    }

    /**
     * @return true in any case
     */
    @Override
    public boolean matches(@Nonnull Run<?, ?> run) {
        return true;
    }

    /**
     * @param state state to set
     * @param msg   message to set. Can contain env vars
     *
     * @return new instance of this conditional result
     */
    public static AnyBuildResult onAnyResult(GHCommitState state, String msg) {
        AnyBuildResult cond = new AnyBuildResult();
        cond.setState(state.name());
        cond.setMessage(msg);
        return cond;
    }

    @Extension
    public static class AnyBuildResultDescriptor extends ConditionalResultDescriptor {
        @Override
        public String getDisplayName() {
            return "result ANY";
        }
    }
}

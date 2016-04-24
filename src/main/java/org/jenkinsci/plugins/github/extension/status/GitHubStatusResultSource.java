package org.jenkinsci.plugins.github.extension.status;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.kohsuke.github.GHCommitState;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Extension point to provide exact state and message for the commit
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public abstract class GitHubStatusResultSource extends AbstractDescribableImpl<GitHubStatusResultSource>
        implements ExtensionPoint {

    /**
     * @param run      actual run
     * @param listener run listener
     *
     * @return bean with state and already expanded message
     */
    public abstract StatusResult get(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener)
            throws IOException, InterruptedException;

    /**
     * Bean with state and msg info
     */
    public static class StatusResult {
        private GHCommitState state;
        private String msg;

        public StatusResult(GHCommitState state, String msg) {
            this.state = state;
            this.msg = msg;
        }

        public GHCommitState getState() {
            return state;
        }

        public String getMsg() {
            return msg;
        }
    }
}

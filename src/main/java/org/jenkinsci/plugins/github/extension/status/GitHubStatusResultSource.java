package org.jenkinsci.plugins.github.extension.status;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.kohsuke.github.GHCommitState;

import javax.annotation.Nonnull;

/**
 * @author lanwen (Merkushev Kirill)
 */
public abstract class GitHubStatusResultSource extends AbstractDescribableImpl<GitHubStatusResultSource>
        implements ExtensionPoint {

    public abstract StatusResult get(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener);

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

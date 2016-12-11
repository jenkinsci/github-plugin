package org.jenkinsci.plugins.github.extension.status;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Extension point to provide commit sha which will be used to set state
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public abstract class GitHubCommitShaSource extends AbstractDescribableImpl<GitHubCommitShaSource>
        implements ExtensionPoint {

    /**
     * @param run      enclosing run
     * @param listener listener of the run. Can be used to fetch env vars
     *
     * @return plain sha to set state
     */
    public abstract String get(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener)
            throws IOException, InterruptedException;
}

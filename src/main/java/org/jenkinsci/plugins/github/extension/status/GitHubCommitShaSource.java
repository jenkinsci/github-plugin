package org.jenkinsci.plugins.github.extension.status;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author lanwen (Merkushev Kirill)
 */
public abstract class GitHubCommitShaSource extends AbstractDescribableImpl<GitHubCommitShaSource>
        implements ExtensionPoint {

    public abstract String get(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener) throws IOException;
}

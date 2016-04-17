package org.jenkinsci.plugins.github.extension.status;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;

/**
 * @author lanwen (Merkushev Kirill)
 */
public abstract class GitHubStatusContextSource extends AbstractDescribableImpl<GitHubStatusContextSource>
        implements ExtensionPoint {

    public abstract String context(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener);
}

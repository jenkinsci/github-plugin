package org.jenkinsci.plugins.github.extension.status;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import hudson.model.TaskListener;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Extension point to provide context of the state. For example `integration-tests` or `build`
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public abstract class GitHubStatusContextSource extends AbstractDescribableImpl<GitHubStatusContextSource>
        implements ExtensionPoint {

    /**
     * @param run      actual run
     * @param listener build listener
     *
     * @return simple short string to represent context of this state
     */
    public abstract String context(@NonNull Run<?, ?> run, @NonNull TaskListener listener);
}

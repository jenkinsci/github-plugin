package org.jenkinsci.plugins.github.extension.status;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.kohsuke.github.GHRepository;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Extension point to provide list of resolved repositories where commit is located
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public abstract class GitHubReposSource extends AbstractDescribableImpl<GitHubReposSource> implements ExtensionPoint {

    /**
     * @param run      actual run
     * @param listener build listener
     *
     * @return resolved list of GitHub repositories
     */
    public abstract List<GHRepository> repos(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener);
}

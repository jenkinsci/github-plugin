package org.jenkinsci.plugins.github.extension.status;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.kohsuke.github.GHRepository;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author lanwen (Merkushev Kirill)
 */
public abstract class GitHubReposSource extends AbstractDescribableImpl<GitHubReposSource> implements ExtensionPoint {

    public abstract List<GHRepository> repos(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener);
}

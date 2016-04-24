package org.jenkinsci.plugins.github.status.sources;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.extension.status.GitHubReposSource;
import org.jenkinsci.plugins.github.util.misc.NullSafeFunction;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * Just uses contributors to get list of resolved repositories
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public class AnyDefinedRepositorySource extends GitHubReposSource {

    @DataBoundConstructor
    public AnyDefinedRepositorySource() {
    }

    /**
     * @return all repositories which can be found by repo-contributors
     */
    @Override
    public List<GHRepository> repos(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener) {
        final Collection<GitHubRepositoryName> names = GitHubRepositoryNameContributor
                .parseAssociatedNames(run.getParent());
        return from(names).transformAndConcat(new NullSafeFunction<GitHubRepositoryName, Iterable<GHRepository>>() {
            @Override
            protected Iterable<GHRepository> applyNullSafe(@Nonnull GitHubRepositoryName name) {
                return name.resolve();
            }
        }).toList();
    }

    @Extension
    public static class AnyDefinedRepoSourceDescriptor extends Descriptor<GitHubReposSource> {
        @Override
        public String getDisplayName() {
            return "Any defined in job repository";
        }
    }
}

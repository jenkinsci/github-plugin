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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
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

    private static final Logger LOG = LoggerFactory.getLogger(AnyDefinedRepositorySource.class);

    @DataBoundConstructor
    public AnyDefinedRepositorySource() {
    }

    /**
     * @return all repositories which can be found by repo-contributors
     */
    @Override
    public List<GHRepository> repos(@NonNull Run<?, ?> run, @NonNull TaskListener listener) {
        final Collection<GitHubRepositoryName> names = GitHubRepositoryNameContributor
                .parseAssociatedNames(run.getParent());

        LOG.trace("repositories source=repo-name-contributor value={}", names);

        return from(names).transformAndConcat(new NullSafeFunction<GitHubRepositoryName, Iterable<GHRepository>>() {
            @Override
            protected Iterable<GHRepository> applyNullSafe(@NonNull GitHubRepositoryName name) {
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

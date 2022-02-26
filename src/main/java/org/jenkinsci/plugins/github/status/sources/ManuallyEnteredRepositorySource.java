package org.jenkinsci.plugins.github.status.sources;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.extension.status.GitHubReposSource;
import org.jenkinsci.plugins.github.util.misc.NullSafeFunction;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.List;

import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

public class ManuallyEnteredRepositorySource extends GitHubReposSource {
    private String url;

    @DataBoundConstructor
    public ManuallyEnteredRepositorySource(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    @VisibleForTesting
    GitHubRepositoryName createName(String url) {
        return GitHubRepositoryName.create(url);
    }

    @Override
    public List<GHRepository> repos(@NonNull Run<?, ?> run, @NonNull final TaskListener listener) {
        List<String> urls = Collections.singletonList(url);
        return from(urls).transformAndConcat(new NullSafeFunction<String, Iterable<GHRepository>>() {
            @Override
            protected Iterable<GHRepository> applyNullSafe(@NonNull String url) {
                GitHubRepositoryName name = createName(url);
                if (name != null) {
                    return name.resolve();
                } else {
                    listener.getLogger().printf("Unable to match %s with a GitHub repository.%n", url);
                    return Collections.emptyList();
                }
            }
        }).toList();
    }

    @Extension
    public static class ManuallyEnteredRepositorySourceDescriptor extends Descriptor<GitHubReposSource> {
        @Override
        public String getDisplayName() {
            return "Manually entered repository";
        }
    }
}

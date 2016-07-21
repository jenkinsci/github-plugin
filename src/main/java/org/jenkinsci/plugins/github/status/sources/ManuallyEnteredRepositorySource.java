package org.jenkinsci.plugins.github.status.sources;

import com.cloudbees.jenkins.GitHubRepositoryName;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.github.extension.status.GitHubReposSource;
import org.jenkinsci.plugins.github.util.misc.NullSafeFunction;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
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

    @Override
    public List<GHRepository> repos(@Nonnull Run<?, ?> run, @Nonnull final TaskListener listener) {
        List<String> urls = Collections.singletonList(url);
        return from(urls).transformAndConcat(new NullSafeFunction<String, Iterable<GHRepository>>() {
            @Override
            protected Iterable<GHRepository> applyNullSafe(@Nonnull String url) {
                GitHubRepositoryName name = GitHubRepositoryName.create(url);
                if (name != null) {
                    return name.resolve();
                } else {
                    listener.getLogger().println("Unable to match " + url + " with a GitHub repository.");
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

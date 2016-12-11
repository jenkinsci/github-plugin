package org.jenkinsci.plugins.github.test;

import com.cloudbees.jenkins.GitHubRepositoryName;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.is;

/**
 * @author lanwen (Merkushev Kirill)
 */
public final class GitHubRepoNameMatchers {
    private GitHubRepoNameMatchers() {
    }

    public static Matcher<String> repo(final Matcher<GitHubRepositoryName> matcher) {
        return new DiagnosingMatcher<String>() {
            @Override
            protected boolean matches(Object url, Description mismatchDescription) {
                mismatchDescription.appendText("for url ").appendValue(url).appendText(" instead of expected repo ");

                if (url != null && !(url instanceof String)) {
                    return false;
                }

                GitHubRepositoryName repo = GitHubRepositoryName.create((String) url);
                matcher.describeMismatch(repo, mismatchDescription);
                return matcher.matches(repo);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("GitHub full repo ").appendDescriptionOf(matcher);
            }
        };
    }

    public static Matcher<GitHubRepositoryName> withHost(String host) {
        return new FeatureMatcher<GitHubRepositoryName, String>(is(host), "with host", "host") {
            @Override
            protected String featureValueOf(GitHubRepositoryName repo) {
                return repo.getHost();
            }
        };
    }

    public static Matcher<GitHubRepositoryName> withUserName(String username) {
        return new FeatureMatcher<GitHubRepositoryName, String>(is(username), "with username", "username") {
            @Override
            protected String featureValueOf(GitHubRepositoryName repo) {
                return repo.getUserName();
            }
        };
    }

    public static Matcher<GitHubRepositoryName> withRepoName(String reponame) {
        return new FeatureMatcher<GitHubRepositoryName, String>(is(reponame), "with reponame", "reponame") {
            @Override
            protected String featureValueOf(GitHubRepositoryName repo) {
                return repo.getRepositoryName();
            }
        };
    }
}

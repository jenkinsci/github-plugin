package org.jenkinsci.plugins.github.test;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;

import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.tokenFor;

/**
 * @author lanwen (Merkushev Kirill)
 */
public final class GitHubServerConfigMatcher {
    private GitHubServerConfigMatcher() {
    }

    public static Matcher<GitHubServerConfig> withApiUrl(Matcher<String> matcher) {
        return new FeatureMatcher<GitHubServerConfig, String>(matcher, "api url", "") {
            @Override
            protected String featureValueOf(GitHubServerConfig actual) {
                return actual.getApiUrl();
            }
        };
    }

    public static Matcher<GitHubServerConfig> withClientCacheSize(Matcher<Integer> matcher) {
        return new FeatureMatcher<GitHubServerConfig, Integer>(matcher, "client cache size", "") {
            @Override
            protected Integer featureValueOf(GitHubServerConfig actual) {
                return actual.getClientCacheSize();
            }
        };
    }

    public static Matcher<GitHubServerConfig> withCredsId(Matcher<String> matcher) {
        return new FeatureMatcher<GitHubServerConfig, String>(matcher, "credentials id", "") {
            @Override
            protected String featureValueOf(GitHubServerConfig actual) {
                return actual.getCredentialsId();
            }
        };
    }

    public static Matcher<GitHubServerConfig> withCredsWithToken(String token) {
        return new FeatureMatcher<GitHubServerConfig, String>(is(token), "token in creds", "") {
            @Override
            protected String featureValueOf(GitHubServerConfig actual) {
                return tokenFor(actual.getCredentialsId());
            }
        };
    }

    public static Matcher<GitHubServerConfig> withIsManageHooks(Matcher<Boolean> matcher) {
        return new FeatureMatcher<GitHubServerConfig, Boolean>(matcher, "is manage hooks", "") {
            @Override
            protected Boolean featureValueOf(GitHubServerConfig actual) {
                return actual.isManageHooks();
            }
        };
    }

    public static Matcher<GitHubServerConfig> withName(Matcher<String> matcher) {
        return new FeatureMatcher<GitHubServerConfig, String>(matcher, "name", "") {
            @Override
            protected String featureValueOf(GitHubServerConfig actual) {
                return actual.getName();
            }
        };
    }
}

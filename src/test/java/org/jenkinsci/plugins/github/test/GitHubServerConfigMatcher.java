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

    public static Matcher<GitHubServerConfig> withCredsWithToken(String token) {
        return new FeatureMatcher<GitHubServerConfig, String>(is(token), "token in creds", "") {
            @Override
            protected String featureValueOf(GitHubServerConfig actual) {
                return tokenFor(actual.getCredentialsId());
            }
        };
    }
}

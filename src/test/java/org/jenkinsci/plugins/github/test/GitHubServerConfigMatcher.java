package org.jenkinsci.plugins.github.test;

import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.model.Mapping;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.tokenFor;

/**
 * @author lanwen (Merkushev Kirill)
 */
public final class GitHubServerConfigMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(GitHubServerConfigMatcher.class);

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

    public static Matcher<Mapping> withApiUrlS(Matcher<String> matcher) {
        return new FeatureMatcher<Mapping, String>(matcher, "api url", "") {
            @Override
            protected String featureValueOf(Mapping actual) {
                return valueOrNull(actual, "apiUrl");
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

    public static Matcher<Mapping> withClientCacheSizeS(Matcher<Integer> matcher) {
        return new FeatureMatcher<Mapping, Integer>(matcher, "client cache size", "") {
            @Override
            protected Integer featureValueOf(Mapping actual) {
                return Integer.valueOf(valueOrNull(actual, "clientCacheSize"));
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

    public static Matcher<Mapping> withCredsIdS(Matcher<String> matcher) {
        return new FeatureMatcher<Mapping, String>(matcher, "credentials id", "") {
            @Override
            protected String featureValueOf(Mapping actual) {
                return valueOrNull(actual, "credentialsId");
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

    public static Matcher<Mapping> withIsManageHooksS(Matcher<Boolean> matcher) {
        return new FeatureMatcher<Mapping, Boolean>(matcher, "is manage hooks", "") {
            @Override
            protected Boolean featureValueOf(Mapping actual) {
                return Boolean.valueOf(valueOrNull(actual, "manageHooks"));
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

    public static Matcher<Mapping> withNameS(Matcher<String> matcher) {
        return new FeatureMatcher<Mapping, String>(matcher, "name", "") {
            @Override
            protected String featureValueOf(Mapping actual) {
                return valueOrNull(actual, "name");
            }
        };
    }

    private static String valueOrNull(Mapping mapping, String key) {
        try {
            return mapping.get(key).asScalar().getValue();
        } catch (NullPointerException | ConfiguratorException e) {
            throw new AssertionError(key);
        }
    }
}

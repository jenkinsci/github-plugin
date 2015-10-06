package org.jenkinsci.plugins.github.internal;

import com.squareup.okhttp.Cache;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.jenkinsci.plugins.github.internal.GitHubClientCacheOps.toCacheDir;
import static org.jenkinsci.plugins.github.internal.GitHubClientCacheOps.withEnabledCache;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubClientCacheOpsTest {

    public static final String CREDENTIALS_ID = "credsid";
    public static final String CREDENTIALS_ID_2 = "credsid2";
    public static final String CUSTOM_API_URL = "http://api.some.unk/";

    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    @Test
    public void shouldPointToSameCacheForOneConfig() throws Exception {
        GitHubServerConfig config = new GitHubServerConfig(CREDENTIALS_ID);

        Cache cache1 = toCacheDir().apply(config);
        Cache cache2 = toCacheDir().apply(config);

        assertThat("same config should get same cache",
                cache1.getDirectory().getAbsolutePath(), equalTo(cache2.getDirectory().getAbsolutePath()));
    }

    @Test
    public void shouldPointToDifferentCachesOnChangedApiPath() throws Exception {
        GitHubServerConfig config = new GitHubServerConfig(CREDENTIALS_ID);
        config.setCustomApiUrl(true);
        config.setApiUrl(CUSTOM_API_URL);

        GitHubServerConfig config2 = new GitHubServerConfig(CREDENTIALS_ID);

        Cache cache1 = toCacheDir().apply(config);
        Cache cache2 = toCacheDir().apply(config2);

        assertThat("with changed url",
                cache1.getDirectory().getAbsolutePath(), not(cache2.getDirectory().getAbsolutePath()));
    }

    @Test
    public void shouldPointToDifferentCachesOnChangedCreds() throws Exception {
        GitHubServerConfig config = new GitHubServerConfig(CREDENTIALS_ID);
        GitHubServerConfig config2 = new GitHubServerConfig(CREDENTIALS_ID_2);

        Cache cache1 = toCacheDir().apply(config);
        Cache cache2 = toCacheDir().apply(config2);

        assertThat("with changed creds",
                cache1.getDirectory().getAbsolutePath(), not(cache2.getDirectory().getAbsolutePath()));
    }

    @Test
    @WithoutJenkins
    public void shouldReturnEnabledOnCacheGreaterThan0() throws Exception {
        GitHubServerConfig config = new GitHubServerConfig(CREDENTIALS_ID);
        config.setClientCacheSize(1);
        
        assertThat("1MB", withEnabledCache().apply(config), is(true));
    }

    @Test
    @WithoutJenkins
    public void shouldReturnNotEnabledOnCacheEq0() throws Exception {
        GitHubServerConfig config = new GitHubServerConfig(CREDENTIALS_ID);
        config.setClientCacheSize(0);
        
        assertThat("zero cache", withEnabledCache().apply(config), is(false));
    }

    @Test
    @WithoutJenkins
    public void shouldReturnNotEnabledOnCacheLessThan0() throws Exception {
        GitHubServerConfig config = new GitHubServerConfig(CREDENTIALS_ID);
        config.setClientCacheSize(-1);
        
        assertThat("-1 value", withEnabledCache().apply(config), is(false));
    }

    @Test
    @WithoutJenkins
    public void shouldHaveEnabledCacheByDefault() throws Exception {
        assertThat("default cache", withEnabledCache().apply(new GitHubServerConfig(CREDENTIALS_ID)), is(true));
    }
}

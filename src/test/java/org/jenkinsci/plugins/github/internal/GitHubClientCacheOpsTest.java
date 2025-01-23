package org.jenkinsci.plugins.github.internal;

import okhttp3.Cache;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.jenkinsci.plugins.github.internal.GitHubClientCacheOps.notInCaches;
import static org.jenkinsci.plugins.github.internal.GitHubClientCacheOps.toCacheDir;
import static org.jenkinsci.plugins.github.internal.GitHubClientCacheOps.withEnabledCache;

/**
 * @author lanwen (Merkushev Kirill)
 */
@WithJenkins
class GitHubClientCacheOpsTest {

    public static final String CREDENTIALS_ID = "credsid";
    public static final String CREDENTIALS_ID_2 = "credsid2";
    public static final String CUSTOM_API_URL = "http://api.some.unk/";

    @TempDir
    public static File tmp;

    private JenkinsRule jRule;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jRule = rule;
    }

    @Test
    void shouldPointToSameCacheForOneConfig() throws Exception {
        GitHubServerConfig config = new GitHubServerConfig(CREDENTIALS_ID);

        Cache cache1 = toCacheDir().apply(config);
        Cache cache2 = toCacheDir().apply(config);

        assertThat("same config should get same cache",
                cache1.directory().getAbsolutePath(), equalTo(cache2.directory().getAbsolutePath()));
    }

    @Test
    void shouldPointToDifferentCachesOnChangedApiPath() throws Exception {
        GitHubServerConfig config = new GitHubServerConfig(CREDENTIALS_ID);
        config.setApiUrl(CUSTOM_API_URL);

        GitHubServerConfig config2 = new GitHubServerConfig(CREDENTIALS_ID);

        Cache cache1 = toCacheDir().apply(config);
        Cache cache2 = toCacheDir().apply(config2);

        assertThat("with changed url",
                cache1.directory().getAbsolutePath(), not(cache2.directory().getAbsolutePath()));
    }

    @Test
    void shouldPointToDifferentCachesOnChangedCreds() throws Exception {
        GitHubServerConfig config = new GitHubServerConfig(CREDENTIALS_ID);
        GitHubServerConfig config2 = new GitHubServerConfig(CREDENTIALS_ID_2);

        Cache cache1 = toCacheDir().apply(config);
        Cache cache2 = toCacheDir().apply(config2);

        assertThat("with changed creds",
                cache1.directory().getAbsolutePath(), not(cache2.directory().getAbsolutePath()));
    }

    @Test
    @WithoutJenkins
    void shouldNotAcceptFilesInFilter() throws Exception {
        assertThat("file should not be accepted",
                notInCaches(newHashSet("file")).accept(File.createTempFile("junit", null, tmp).toPath()), is(false));
    }

    @Test
    @WithoutJenkins
    void shouldNotAcceptDirsInFilterWithNameFromSet() throws Exception {
        File dir = newFolder(tmp, "junit");
        assertThat("should not accept folders from set",
                notInCaches(newHashSet(dir.getName())).accept(dir.toPath()), is(false));
    }

    @Test
    @WithoutJenkins
    void shouldAcceptDirsInFilterWithNameNotInSet() throws Exception {
        File dir = newFolder(tmp, "junit");
        assertThat("should accept folders not in set",
                notInCaches(newHashSet(dir.getName() + "abc")).accept(dir.toPath()), is(true));
    }

    @Test
    @WithoutJenkins
    void shouldReturnEnabledOnCacheGreaterThan0() throws Exception {
        GitHubServerConfig config = new GitHubServerConfig(CREDENTIALS_ID);
        config.setClientCacheSize(1);

        assertThat("1MB", withEnabledCache().apply(config), is(true));
    }

    @Test
    @WithoutJenkins
    void shouldReturnNotEnabledOnCacheEq0() throws Exception {
        GitHubServerConfig config = new GitHubServerConfig(CREDENTIALS_ID);
        config.setClientCacheSize(0);

        assertThat("zero cache", withEnabledCache().apply(config), is(false));
    }

    @Test
    @WithoutJenkins
    void shouldReturnNotEnabledOnCacheLessThan0() throws Exception {
        GitHubServerConfig config = new GitHubServerConfig(CREDENTIALS_ID);
        config.setClientCacheSize(-1);

        assertThat("-1 value", withEnabledCache().apply(config), is(false));
    }

    @Test
    @WithoutJenkins
    void shouldHaveEnabledCacheByDefault() throws Exception {
        assertThat("default cache", withEnabledCache().apply(new GitHubServerConfig(CREDENTIALS_ID)), is(true));
    }

    private static File newFolder(File root, String... subDirs) {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        result.mkdirs();
        return result;
    }
}

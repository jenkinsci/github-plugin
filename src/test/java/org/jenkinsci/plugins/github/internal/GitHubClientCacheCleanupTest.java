package org.jenkinsci.plugins.github.internal;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.jenkinsci.plugins.github.test.GHMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.google.common.collect.Lists.newArrayList;
import static java.nio.file.Files.newDirectoryStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.jenkinsci.plugins.github.internal.GitHubClientCacheOps.clearRedundantCaches;
import static org.jenkinsci.plugins.github.internal.GitHubClientCacheOps.getBaseCacheDir;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubClientCacheCleanupTest {

    public static final String DEFAULT_CREDS_ID = "";
    public static final String CHANGED_CREDS_ID = "id";

    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    @Rule
    public GHMockRule github = new GHMockRule(new WireMockRule(wireMockConfig().dynamicPort())).stubUser();


    @Test
    public void shouldCreateCachedFolder() throws Exception {
        makeCachedRequestWithCredsId(DEFAULT_CREDS_ID);

        it("should create cached dir", 1);
    }

    @Test
    public void shouldCreateOnlyOneCachedFolderForSameCredsAndApi() throws Exception {
        makeCachedRequestWithCredsId(DEFAULT_CREDS_ID);
        makeCachedRequestWithCredsId(DEFAULT_CREDS_ID);

        it("should create and use same cached dir", 1);
    }

    @Test
    public void shouldCreateCachedFolderForEachCreds() throws Exception {
        makeCachedRequestWithCredsId(DEFAULT_CREDS_ID);
        makeCachedRequestWithCredsId(CHANGED_CREDS_ID);

        it("should create cached dirs for each config", 2);
    }

    @Test
    public void shouldRemoveCachedDirAfterClean() throws Exception {
        makeCachedRequestWithCredsId(DEFAULT_CREDS_ID);

        clearRedundantCaches(Collections.<GitHubServerConfig>emptyList());

        it("should remove cached dir", 0);
    }

    @Test
    public void shouldRemoveOnlyNotActiveCachedDirAfterClean() throws Exception {
        makeCachedRequestWithCredsId(DEFAULT_CREDS_ID);
        makeCachedRequestWithCredsId(CHANGED_CREDS_ID);

        GitHubServerConfig config = new GitHubServerConfig(CHANGED_CREDS_ID);
        config.setApiUrl(github.serverConfig().getApiUrl());
        config.setClientCacheSize(1);

        clearRedundantCaches(newArrayList(config));

        it("should remove only not active cache dir", 1);
    }

    @Test
    public void shouldRemoveCacheWhichNotEnabled() throws Exception {
        makeCachedRequestWithCredsId(CHANGED_CREDS_ID);

        GitHubServerConfig config = new GitHubServerConfig(CHANGED_CREDS_ID);
        config.setApiUrl(github.serverConfig().getApiUrl());
        config.setClientCacheSize(0);

        clearRedundantCaches(newArrayList(config));

        it("should remove not active cache dir", 0);
    }

    private void it(String comment, int count) throws IOException {
        try (DirectoryStream<Path> paths = newDirectoryStream(getBaseCacheDir())) {
            assertThat(comment, newArrayList(paths), hasSize(count));
        }
    }

    private void makeCachedRequestWithCredsId(String credsId) throws IOException {
        GitHubServerConfig config = new GitHubServerConfig(credsId);
        config.setApiUrl(github.serverConfig().getApiUrl());
        config.setClientCacheSize(1);
        GitHub gitHub = GitHubServerConfig.loginToGithub().apply(config);
        gitHub.getMyself();
    }
}

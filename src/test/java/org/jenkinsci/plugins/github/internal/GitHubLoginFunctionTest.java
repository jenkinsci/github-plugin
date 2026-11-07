package org.jenkinsci.plugins.github.internal;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import hudson.Functions;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@WithJenkins
class GitHubLoginFunctionTest {

    private static final String REPO_PATH = "/repos/org/repo";
    private static final String REPO_JSON =
            "{\"id\": 1, \"name\": \"repo\", \"full_name\": \"org/repo\", \"owner\": {\"login\": \"org\"}}";

    @RegisterExtension
    static WireMockExtension githubApi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private JenkinsRule jRule;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jRule = rule;
    }

    @Test
    void requestsGoThroughTheClientInterceptors() throws Exception {
        githubApi.stubFor(get(urlEqualTo(REPO_PATH)).willReturn(okJson(REPO_JSON)));
        List<String> application = new CopyOnWriteArrayList<>();
        List<String> network = new CopyOnWriteArrayList<>();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    application.add(chain.request().method() + " " + chain.request().url().encodedPath());
                    return chain.proceed(chain.request());
                })
                .addNetworkInterceptor(chain -> {
                    Response response = chain.proceed(chain.request());
                    network.add(chain.request().method() + " " + chain.request().url().encodedPath()
                            + " " + response.code());
                    return response;
                })
                .build();
        GitHubServerConfig config = serverConfig();
        config.setClientCacheSize(0);

        GitHub github = new GitHubLoginFunction(client).apply(config);
        assertNotNull(github);
        assertEquals("repo", github.getRepository("org/repo").getName());

        assertThat(application, hasItem("GET " + REPO_PATH));
        assertThat(network, hasItem("GET " + REPO_PATH + " 200"));
    }

    @Test
    void cachedResponseIsRevalidatedWithETag() throws Exception {
        // like GitHubClientCacheCleanupTest: the disk cache in JENKINS_HOME stays open on Windows
        assumeFalse(Functions.isWindows(), "the open disk cache keeps JENKINS_HOME from being deleted on Windows");
        githubApi.stubFor(get(urlEqualTo(REPO_PATH))
                .withHeader("If-None-Match", absent())
                .willReturn(okJson(REPO_JSON).withHeader("ETag", "\"v1\"")));
        githubApi.stubFor(get(urlEqualTo(REPO_PATH))
                .withHeader("If-None-Match", equalTo("\"v1\""))
                .willReturn(aResponse().withStatus(304).withHeader("ETag", "\"v1\"")));
        GitHubServerConfig config = serverConfig();
        config.setClientCacheSize(1);

        GitHub github = new GitHubLoginFunction().apply(config);
        assertNotNull(github);
        assertEquals("repo", github.getRepository("org/repo").getName());
        assertEquals("repo", github.getRepository("org/repo").getName());

        // Every request asks GitHub (max-age=0); the second one is conditional and is answered from the cache.
        githubApi.verify(2, getRequestedFor(urlEqualTo(REPO_PATH)).withHeader("Cache-Control", equalTo("max-age=0")));
        githubApi.verify(1, getRequestedFor(urlEqualTo(REPO_PATH)).withHeader("If-None-Match", equalTo("\"v1\"")));
    }

    @Test
    void rateLimitExceededFails() throws Exception {
        githubApi.stubFor(get(urlEqualTo(REPO_PATH))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader("Content-Type", "application/json")
                        .withHeader("X-RateLimit-Limit", "5000")
                        .withHeader("X-RateLimit-Remaining", "0")
                        .withHeader("X-RateLimit-Reset", Long.toString(System.currentTimeMillis() / 1000 + 3600))
                        .withBody("{\"message\": \"API rate limit exceeded\"}")));
        GitHubServerConfig config = serverConfig();
        config.setClientCacheSize(0);

        GitHub github = new GitHubLoginFunction().apply(config);
        assertNotNull(github);
        HttpException e = assertThrows(HttpException.class, () -> github.getRepository("org/repo"));

        assertEquals(403, e.getResponseCode());
        githubApi.verify(1, getRequestedFor(urlEqualTo(REPO_PATH)));
    }

    private static GitHubServerConfig serverConfig() {
        GitHubServerConfig config = new GitHubServerConfig("creds");
        config.setApiUrl(githubApi.baseUrl());
        return config;
    }
}

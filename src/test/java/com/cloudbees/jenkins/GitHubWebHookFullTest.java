package com.cloudbees.jenkins;

import com.google.common.base.Charsets;
import jakarta.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.webhook.GHEventHeader;
import org.jenkinsci.plugins.github.webhook.GHEventPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.github.GHEvent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static java.lang.String.format;
import static org.apache.commons.lang3.ClassUtils.PACKAGE_SEPARATOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.jenkinsci.plugins.github.test.HookSecretHelper.removeSecretIn;
import static org.jenkinsci.plugins.github.test.HookSecretHelper.storeSecretIn;
import static org.jenkinsci.plugins.github.webhook.RequirePostWithGHHookPayload.Processor.SHA256_PREFIX;
import static org.jenkinsci.plugins.github.webhook.RequirePostWithGHHookPayload.Processor.SIGNATURE_HEADER;
import static org.jenkinsci.plugins.github.webhook.RequirePostWithGHHookPayload.Processor.SIGNATURE_HEADER_SHA256;

/**
 * @author lanwen (Merkushev Kirill)
 */
@WithJenkins
public class GitHubWebHookFullTest {

    public static final String APPLICATION_JSON = GHEventPayload.PayloadHandler.APPLICATION_JSON;
    public static final String FORM = GHEventPayload.PayloadHandler.FORM_URLENCODED;

    @Inject
    private GitHubPluginConfig config;

    private JenkinsRule jenkins;
    private HttpClient httpClient;

    @BeforeEach
    void before(JenkinsRule rule) throws Throwable {
        jenkins = rule;
        jenkins.getInstance().getInjector().injectMembers(this);
        httpClient = HttpClient.newHttpClient();
    }

    @Test
    void shouldParseJsonWebHookFromGH() throws Exception {
        removeSecretIn(config);
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(getPath()))
                        .POST(HttpRequest.BodyPublishers.ofString(classpath("payloads/push.json")))
                        .header("Content-Type", APPLICATION_JSON)
                        .header(GHEventHeader.PayloadHandler.EVENT_HEADER, GHEvent.PUSH.name().toLowerCase())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat("status", response.statusCode(), is(SC_OK));
    }

    @Test
    void shouldParseJsonWebHookFromGHWithSignHeader() throws Exception {
        String hash = "355e155fc3d10c4e5f2c6086a01281d2e947d932";
        String hash256 = "85e61999573c7023720a12375e1e55d18a0870e1ef880736f6ffc9273d0519e3";
        String secret = "123";

        storeSecretIn(config, secret);
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(getPath()))
                        .POST(HttpRequest.BodyPublishers.ofString(
                                classpath(format("payloads/ping_hash_%s_secret_%s.json", hash, secret))))
                        .header("Content-Type", APPLICATION_JSON)
                        .header(GHEventHeader.PayloadHandler.EVENT_HEADER, GHEvent.PUSH.name().toLowerCase())
                        .header(SIGNATURE_HEADER, format("sha1=%s", hash))
                        .header(SIGNATURE_HEADER_SHA256, format("%s%s", SHA256_PREFIX, hash256))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat("status", response.statusCode(), is(SC_OK));
    }

    @Test
    void shouldParseFormWebHookOrServiceHookFromGH() throws Exception {
        String encoded = "payload=" + java.net.URLEncoder.encode(classpath("payloads/push.json"), "UTF-8");
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(getPath()))
                        .POST(HttpRequest.BodyPublishers.ofString(encoded))
                        .header("Content-Type", FORM)
                        .header(GHEventHeader.PayloadHandler.EVENT_HEADER, GHEvent.PUSH.name().toLowerCase())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat("status", response.statusCode(), is(SC_OK));
    }

    @Test
    void shouldParsePingFromGH() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(getPath()))
                        .POST(HttpRequest.BodyPublishers.ofString(classpath("payloads/ping.json")))
                        .header("Content-Type", APPLICATION_JSON)
                        .header(GHEventHeader.PayloadHandler.EVENT_HEADER, GHEvent.PING.name().toLowerCase())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat("status", response.statusCode(), is(SC_OK));
    }

    @Test
    void shouldReturnErrOnEmptyPayloadAndHeader() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(getPath()))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat("status", response.statusCode(), is(SC_BAD_REQUEST));
        assertThat("body", response.body(), containsString("Hook should contain event type"));
    }

    @Test
    void shouldReturnErrOnEmptyPayload() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(getPath()))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .header(GHEventHeader.PayloadHandler.EVENT_HEADER, GHEvent.PUSH.name().toLowerCase())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat("status", response.statusCode(), is(SC_BAD_REQUEST));
        assertThat("body", response.body(), containsString("Hook should contain payload"));
    }

    @Test
    void shouldReturnErrOnGetReq() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(getPath()))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat("status", response.statusCode(), is(SC_METHOD_NOT_ALLOWED));
    }

    @Test
    void shouldProcessSelfTest() throws Exception {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(getPath()))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .header(GitHubWebHook.URL_VALIDATION_HEADER, "nonnull")
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat("status", response.statusCode(), is(SC_OK));
        assertThat("identity header", response.headers().firstValue(GitHubWebHook.X_INSTANCE_IDENTITY).orElse(null), notNullValue());
    }

    private String getPath() {
        return jenkins.getInstance().getRootUrl() + GitHubWebHook.URLNAME.concat("/");
    }

    public static String classpath(String path) {
        return classpath(GitHubWebHookFullTest.class, path);
    }

    public static String classpath(Class<?> clazz, String path) {
        try {
            return IOUtils.toString(clazz.getClassLoader().getResourceAsStream(
                    clazz.getName().replace(PACKAGE_SEPARATOR, File.separator) + File.separator + path
            ), Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(format("Can't load %s for class %s", path, clazz), e);
        }
    }
}

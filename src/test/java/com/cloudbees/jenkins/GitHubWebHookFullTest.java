package com.cloudbees.jenkins;

import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
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

import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static java.lang.String.format;
import static org.apache.commons.lang3.ClassUtils.PACKAGE_SEPARATOR;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.jenkinsci.plugins.github.test.HookSecretHelper.removeSecretIn;
import static org.jenkinsci.plugins.github.test.HookSecretHelper.storeSecretIn;
import static org.jenkinsci.plugins.github.webhook.RequirePostWithGHHookPayload.Processor.SIGNATURE_HEADER;

/**
 * @author lanwen (Merkushev Kirill)
 */
@WithJenkins
public class GitHubWebHookFullTest {

    // GitHub doesn't send the charset per docs, so re-use the exact content-type from the handler
    public static final String APPLICATION_JSON = GHEventPayload.PayloadHandler.APPLICATION_JSON;
    public static final String FORM = GHEventPayload.PayloadHandler.FORM_URLENCODED;

    public static final Header JSON_CONTENT_TYPE = new Header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
    public static final Header FORM_CONTENT_TYPE = new Header(HttpHeaders.CONTENT_TYPE, FORM);
    public static final String NOT_NULL_VALUE = "nonnull";

    private RequestSpecification spec;

    @Inject
    private GitHubPluginConfig config;

    private JenkinsRule jenkins;

    @BeforeEach
    void before(JenkinsRule rule) throws Throwable {
        jenkins = rule;
        jenkins.getInstance().getInjector().injectMembers(this);

        spec = new RequestSpecBuilder()
                .setConfig(newConfig()
                        .encoderConfig(encoderConfig()
                                .defaultContentCharset(Charsets.UTF_8.name())
                                // GitHub doesn't add charsets, so don't test with them
                                .appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                .build();
    }

    @Test
    void shouldParseJsonWebHookFromGH() throws Exception {
        removeSecretIn(config);
        given().spec(spec)
                .header(eventHeader(GHEvent.PUSH))
                .header(JSON_CONTENT_TYPE)
                .body(classpath("payloads/push.json"))
                .log().all()
                .expect().log().all().statusCode(SC_OK).request().post(getPath());
    }


    @Test
    void shouldParseJsonWebHookFromGHWithSignHeader() throws Exception {
        String hash = "355e155fc3d10c4e5f2c6086a01281d2e947d932";
        String secret = "123";

        storeSecretIn(config, secret);
        given().spec(spec)
                .header(eventHeader(GHEvent.PUSH))
                .header(JSON_CONTENT_TYPE)
                .header(SIGNATURE_HEADER, format("sha1=%s", hash))
                .body(classpath(String.format("payloads/ping_hash_%s_secret_%s.json", hash, secret)))
                .log().all()
                .expect().log().all().statusCode(SC_OK).request().post(getPath());
    }

    @Test
    void shouldParseFormWebHookOrServiceHookFromGH() throws Exception {
        given().spec(spec)
                .header(eventHeader(GHEvent.PUSH))
                .header(FORM_CONTENT_TYPE)
                .formParam("payload", classpath("payloads/push.json"))
                .log().all()
                .expect().log().all().statusCode(SC_OK).request().post(getPath());
    }

    @Test
    void shouldParsePingFromGH() throws Exception {
        given().spec(spec)
                .header(eventHeader(GHEvent.PING))
                .header(JSON_CONTENT_TYPE)
                .body(classpath("payloads/ping.json"))
                .log().all()
                .expect().log().all()
                .statusCode(SC_OK)
                .request()
                .post(getPath());
    }

    @Test
    void shouldReturnErrOnEmptyPayloadAndHeader() throws Exception {
        given().spec(spec)
                .log().all()
                .expect().log().all()
                .statusCode(SC_BAD_REQUEST)
                .body(containsString("Hook should contain event type"))
                .request()
                .post(getPath());
    }

    @Test
    void shouldReturnErrOnEmptyPayload() throws Exception {
        given().spec(spec)
                .header(eventHeader(GHEvent.PUSH))
                .log().all()
                .expect().log().all()
                .statusCode(SC_BAD_REQUEST)
                .body(containsString("Hook should contain payload"))
                .request()
                .post(getPath());
    }

    @Test
    void shouldReturnErrOnGetReq() throws Exception {
        given().spec(spec)
                .log().all().expect().log().all()
                .statusCode(SC_METHOD_NOT_ALLOWED)
                .request()
                .get(getPath());
    }

    @Test
    void shouldProcessSelfTest() throws Exception {
        given().spec(spec)
                .header(new Header(GitHubWebHook.URL_VALIDATION_HEADER, NOT_NULL_VALUE))
                .log().all()
                .expect().log().all()
                .statusCode(SC_OK)
                .header(GitHubWebHook.X_INSTANCE_IDENTITY, notNullValue())
                .request()
                .post(getPath());
    }

    public Header eventHeader(GHEvent event) {
        return eventHeader(event.name().toLowerCase());
    }

    public Header eventHeader(String event) {
        return new Header(GHEventHeader.PayloadHandler.EVENT_HEADER, event);
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

    private String getPath(){
        return jenkins.getInstance().getRootUrl() + GitHubWebHook.URLNAME.concat("/");
    }
}

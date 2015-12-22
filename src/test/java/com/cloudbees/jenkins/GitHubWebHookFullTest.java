package com.cloudbees.jenkins;

import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.response.Header;
import com.jayway.restassured.specification.RequestSpecification;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.github.webhook.GHEventHeader;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.github.GHEvent;

import java.io.File;
import java.io.IOException;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.config.EncoderConfig.encoderConfig;
import static com.jayway.restassured.config.RestAssuredConfig.newConfig;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang3.ClassUtils.PACKAGE_SEPARATOR;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubWebHookFullTest {

    public static final String APPLICATION_JSON = "application/json";
    public static final String FORM = "application/x-www-form-urlencoded";

    public static final Header JSON_CONTENT_TYPE = new Header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
    public static final Header FORM_CONTENT_TYPE = new Header(HttpHeaders.CONTENT_TYPE, FORM);
    public static final String NOT_NULL_VALUE = "nonnull";

    private RequestSpecification spec;

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public ExternalResource setup = new ExternalResource() {
        @Override
        protected void before() throws Throwable {
            spec = new RequestSpecBuilder()
                    .setBaseUri(jenkins.getInstance().getRootUrl())
                    .setBasePath(GitHubWebHook.URLNAME.concat("/"))
                    .setConfig(newConfig()
                            .encoderConfig(encoderConfig()
                                    .defaultContentCharset(Charsets.UTF_8)
                                    .appendDefaultContentCharsetToContentTypeIfUndefined(false)))
                    .build();
        }
    };

    @Test
    public void shouldParseJsonWebHookFromGH() throws Exception {
        given().spec(spec)
                .header(eventHeader(GHEvent.PUSH))
                .header(JSON_CONTENT_TYPE)
                .content(classpath("payloads/push.json"))
                .log().all()
                .expect().log().all().statusCode(SC_OK).post();
    }

    @Test
    public void shouldParseFormWebHookOrServiceHookFromGH() throws Exception {
        given().spec(spec)
                .header(eventHeader(GHEvent.PUSH))
                .header(FORM_CONTENT_TYPE)
                .formParam("payload", classpath("payloads/push.json"))
                .log().all()
                .expect().log().all().statusCode(SC_OK).post();
    }

    @Test
    public void shouldParsePingFromGH() throws Exception {
        given().spec(spec)
                .header(eventHeader(GHEvent.PING))
                .header(JSON_CONTENT_TYPE)
                .content(classpath("payloads/ping.json"))
                .log().all()
                .expect().log().all()
                .statusCode(SC_OK)
                .post();
    }

    @Test
    public void shouldReturnErrOnEmptyPayloadAndHeader() throws Exception {
        given().spec(spec)
                .log().all()
                .expect().log().all()
                .statusCode(SC_BAD_REQUEST)
                .body(containsString("Hook should contain event type"))
                .post();
    }

    @Test
    public void shouldReturnErrOnEmptyPayload() throws Exception {
        given().spec(spec)
                .header(eventHeader(GHEvent.PUSH))
                .log().all()
                .expect().log().all()
                .statusCode(SC_BAD_REQUEST)
                .body(containsString("Hook should contain payload"))
                .post();
    }

    @Test
    public void shouldReturnErrOnGetReq() throws Exception {
        given().spec(spec)
                .log().all().expect().log().all()
                .statusCode(SC_METHOD_NOT_ALLOWED)
                .get();
    }

    @Test
    public void shouldProcessSelfTest() throws Exception {
        given().spec(spec)
                .header(new Header(GitHubWebHook.URL_VALIDATION_HEADER, NOT_NULL_VALUE))
                .log().all()
                .expect().log().all()
                .statusCode(SC_OK)
                .header(GitHubWebHook.X_INSTANCE_IDENTITY, notNullValue())
                .post();
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
}

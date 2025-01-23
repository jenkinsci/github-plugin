package org.jenkinsci.plugins.github.test;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import hudson.model.Item;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.cloudbees.jenkins.GitHubWebHookFullTest.classpath;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Mocks GitHub on localhost with some predefined methods
 *
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubMockExtension extends WireMockExtension {

    /**
     * This repo is used in resource files
     */
    public static final GitHubRepositoryName REPO = new GitHubRepositoryName("localhost", "org", "repo");

    /**
     * List of additional stubs. Launched after wiremock has been started
     */
    private final List<Runnable> setups = new ArrayList<>();

    public GitHubMockExtension(Builder builder) {
        super(builder);
    }

    @Override
    protected void onBeforeEach(WireMockRuntimeInfo wireMockRuntimeInfo) {
        super.onBeforeAll(wireMockRuntimeInfo);

        for (Runnable setup : setups) {
            setup.run();
        }
    }

    /**
     * Ready-to-use global config with wiremock service. Just add it to plugin config
     * {@code GitHubPlugin.configuration().getConfigs().add(github.serverConfig());}
     *
     * @return part of global plugin config
     */
    public GitHubServerConfig serverConfig() {
        GitHubServerConfig conf = new GitHubServerConfig("creds");
        conf.setApiUrl("http://localhost:" + getPort());
        return conf;
    }

    /**
     * Stubs /user response with predefined content
     * <p>
     * More info: https://developer.github.com/v3/users/#get-the-authenticated-user
     */
    public GitHubMockExtension stubUser() {
        setups.add(() ->
                stubFor(get(urlPathEqualTo("/user"))
                        .willReturn(aResponse()
                                .withStatus(HTTP_OK)
                                .withHeader("Content-Type", "application/json; charset=utf-8")
                                .withBody(classpath(GitHubMockExtension.class, "user.json")))));
        return this;
    }

    /**
     * Stubs /repos/org/repo response with predefined content
     * <p>
     * More info: https://developer.github.com/v3/repos/#get
     */
    public GitHubMockExtension stubRepo() {
        setups.add(() ->
                stubFor(get(urlPathMatching(format("/repos/%s/%s", REPO.getUserName(), REPO.getRepositoryName())))
                        .willReturn(aResponse()
                                .withStatus(HTTP_OK)
                                .withHeader("Content-Type", "application/json; charset=utf-8")
                                .withBody(classpath(GitHubMockExtension.class, "repos-repo.json")))));
        return this;
    }

    /**
     * Returns 201 CREATED on POST to statuses endpoint (but without content)
     * <p>
     * More info: https://developer.github.com/v3/repos/statuses/
     */
    public GitHubMockExtension stubStatuses() {
        setups.add(() ->
                stubFor(post(urlPathMatching(format("/repos/%s/%s/statuses/.*", REPO.getUserName(), REPO.getRepositoryName())))
                        .willReturn(aResponse()
                                .withStatus(HTTP_CREATED))));
        return this;
    }

    /**
     * Adds predefined repo to list which job can return. This is useful to avoid SCM usage.
     * <p>
     * {@code  @TestExtension
     * public static final FixedGHRepoNameTestContributor CONTRIBUTOR = new FixedGHRepoNameTestContributor();
     * }
     */
    public static class FixedGHRepoNameTestContributor extends GitHubRepositoryNameContributor {
        @Override
        public void parseAssociatedNames(Item job, Collection<GitHubRepositoryName> result) {
            result.add(GitHubMockExtension.REPO);
        }
    }

}

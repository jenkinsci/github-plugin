package org.jenkinsci.plugins.github.config;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainSpecification;
import com.cloudbees.plugins.credentials.domains.HostnameSpecification;
import com.cloudbees.plugins.credentials.domains.SchemeSpecification;
import com.google.common.collect.ImmutableList;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.kohsuke.github.GHAuthorization;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.firstOrNull;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.cloudbees.plugins.credentials.domains.URIRequirementBuilder.fromUri;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.Validate.notNull;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.GITHUB_URL;
import static org.kohsuke.github.GHAuthorization.AMIN_HOOK;
import static org.kohsuke.github.GHAuthorization.REPO;
import static org.kohsuke.github.GHAuthorization.REPO_STATUS;


/**
 * Helper class to convert username+password credentials or directly login+password to GH token
 * and save it as token credentials with help of plain-credentials plugin
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.13.0
 */
@Extension
public class GitHubTokenCredentialsCreator extends Descriptor<GitHubTokenCredentialsCreator> implements
        Describable<GitHubTokenCredentialsCreator> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubTokenCredentialsCreator.class);

    /**
     * Default scope required for this plugin.
     *
     * - admin:repo_hook - for managing hooks (read, write and delete old ones)
     * - repo - to see private repos
     * - repo:status - to manipulate commit statuses
     */
    public static final List<String> GH_PLUGIN_REQUIRED_SCOPE = ImmutableList.of(
            AMIN_HOOK,
            REPO,
            REPO_STATUS
    );

    public GitHubTokenCredentialsCreator() {
        super(GitHubTokenCredentialsCreator.class);
    }

    @Override
    public GitHubTokenCredentialsCreator getDescriptor() {
        return this;
    }

    @Override
    public String getDisplayName() {
        return "Convert login and password to token";
    }

    @SuppressWarnings("unused")
    public ListBoxModel doFillCredentialsIdItems(@QueryParameter String apiUrl, @QueryParameter String credentialsId) {
        if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
            return new StandardUsernameListBoxModel().includeCurrentValue(credentialsId);
        }
        return new StandardUsernameListBoxModel()
                .includeEmptyValue()
                .includeMatchingAs(
                        ACL.SYSTEM,
                        Jenkins.getInstance(),
                        StandardUsernamePasswordCredentials.class,
                        fromUri(defaultIfBlank(apiUrl, GITHUB_URL)).build(),
                        CredentialsMatchers.always()
                )
                .includeMatchingAs(
                        Jenkins.getAuthentication(),
                        Jenkins.getInstance(),
                        StandardUsernamePasswordCredentials.class,
                        fromUri(defaultIfBlank(apiUrl, GITHUB_URL)).build(),
                        CredentialsMatchers.always()
                );
    }

    @SuppressWarnings("unused")
    public FormValidation doCreateTokenByCredentials(
            @QueryParameter String apiUrl,
            @QueryParameter String credentialsId) {

        if (isEmpty(credentialsId)) {
            return FormValidation.error("Please specify credentials to create token");
        }

        StandardUsernamePasswordCredentials creds = firstOrNull(lookupCredentials(
                        StandardUsernamePasswordCredentials.class,
                        Jenkins.getInstance(),
                        ACL.SYSTEM,
                fromUri(defaultIfBlank(apiUrl, GITHUB_URL)).build()),
                withId(credentialsId));
        if (creds == null) {
            // perhaps they selected a personal credential for convertion
            creds = firstOrNull(lookupCredentials(
                    StandardUsernamePasswordCredentials.class,
                    Jenkins.getInstance(),
                    Jenkins.getAuthentication(),
                    fromUri(defaultIfBlank(apiUrl, GITHUB_URL)).build()),
                    withId(credentialsId));
        }

        GHAuthorization token;

        try {
            token = createToken(
                    notNull(creds, "Why selected creds is null?").getUsername(),
                    creds.getPassword().getPlainText(),
                    defaultIfBlank(apiUrl, GITHUB_URL)
            );
        } catch (IOException e) {
            return FormValidation.error(e, "Can't create GH token - %s", e.getMessage());
        }

        StandardCredentials credentials = createCredentials(apiUrl, token.getToken(), creds.getUsername());

        return FormValidation.ok("Created credentials with id %s (can use it for GitHub Server Config)",
                credentials.getId());
    }

    @SuppressWarnings("unused")
    public FormValidation doCreateTokenByPassword(
            @QueryParameter String apiUrl,
            @QueryParameter String login,
            @QueryParameter String password) {

        try {
            GHAuthorization token = createToken(login, password, defaultIfBlank(apiUrl, GITHUB_URL));
            StandardCredentials credentials = createCredentials(apiUrl, token.getToken(), login);

            return FormValidation.ok(
                    "Created credentials with id %s (can use it for GitHub Server Config)",
                    credentials.getId());
        } catch (IOException e) {
            return FormValidation.error(e, "Can't create GH token for %s - %s", login, e.getMessage());
        }
    }

    /**
     * Can be used to convert given login and password to GH personal token as more secured way to interact with api
     *
     * @param username gh login
     * @param password gh password
     * @param apiUrl   gh api url. Can be null or empty to default
     *
     * @return personal token with requested scope
     * @throws IOException when can't create token with given creds
     */
    public GHAuthorization createToken(@Nonnull String username,
                                       @Nonnull String password,
                                       @Nullable String apiUrl) throws IOException {
        GitHub gitHub = new GitHubBuilder()
                .withEndpoint(defaultIfBlank(apiUrl, GITHUB_URL))
                .withPassword(username, password)
                .build();

        return gitHub.createToken(
                GH_PLUGIN_REQUIRED_SCOPE,
                format("Jenkins GitHub Plugin token (%s)", Jenkins.getInstance().getRootUrl()),
                Jenkins.getInstance().getRootUrl()
        );
    }

    /**
     * Creates {@link org.jenkinsci.plugins.plaincredentials.StringCredentials} with previously created GH token.
     * Adds them to domain extracted from server url (will be generated if no any exists before).
     * Domain will have domain requirements consists of scheme and host from serverAPIUrl arg
     *
     * @param serverAPIUrl to add to domain with host and scheme requirement from this url
     * @param token        GH Personal token
     * @param username     used to add to description of newly created creds
     *
     * @return credentials object
     * @see #createCredentials(String, StandardCredentials)
     */
    public StandardCredentials createCredentials(@Nullable String serverAPIUrl, String token, String username) {
        String url = defaultIfBlank(serverAPIUrl, GITHUB_URL);
        String description = format("GitHub (%s) auto generated token credentials for %s", url, username);
        StringCredentialsImpl creds = new StringCredentialsImpl(
                CredentialsScope.GLOBAL,
                UUID.randomUUID().toString(),
                description,
                Secret.fromString(token));
        return createCredentials(url, creds);
    }

    /**
     * Saves given creds in jenkins for domain extracted from server api url
     *
     * @param serverAPIUrl to extract (and create if no any) domain
     * @param credentials  creds to save
     *
     * @return saved creds
     */
    private StandardCredentials createCredentials(@Nonnull String serverAPIUrl,
                                                  final StandardCredentials credentials) {
        URI serverUri = URI.create(defaultIfBlank(serverAPIUrl, GITHUB_URL));

        List<DomainSpecification> specifications = asList(
                new SchemeSpecification(serverUri.getScheme()),
                new HostnameSpecification(serverUri.getHost(), null)
        );

        final Domain domain = new Domain(serverUri.getHost(), "GitHub domain (autogenerated)", specifications);
        ACL.impersonate(ACL.SYSTEM, new Runnable() { // do it with system rights
            @Override
            public void run() {
                try {
                    new SystemCredentialsProvider.StoreImpl().addDomain(domain, credentials);
                } catch (IOException e) {
                    LOGGER.error("Can't add creds for domain", e);
                }
            }
        });

        return credentials;
    }
}

package org.jenkinsci.plugins.github.config;

import com.cloudbees.jenkins.GitHubWebHook;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.internal.GHPluginConfigException;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jenkinsci.plugins.github.config.GitHubServerConfig.allowedToManageHooks;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.loginToGithub;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * Global configuration to store all GH Plugin settings
 * such as hook managing policy, credentials etc.
 *
 * @author lanwen (Merkushev Kirill)
 * @since TODO
 */
public class GitHubPluginConfig extends AbstractDescribableImpl<GitHubPluginConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPluginConfig.class);

    private List<GitHubServerConfig> configs = new ArrayList<GitHubServerConfig>();
    private URL hookUrl;
    private transient boolean overrideHookUrl;

    @DataBoundConstructor
    public GitHubPluginConfig() {
    }

    public List<GitHubServerConfig> getConfigs() {
        return configs;
    }

    @DataBoundSetter
    public void setConfigs(List<GitHubServerConfig> configs) {
        this.configs = configs;
    }

    public boolean isManageHooks() {
        return from(getConfigs()).filter(allowedToManageHooks()).first().isPresent();
    }

    @DataBoundSetter
    public void setHookUrl(URL hookUrl) {
        if (overrideHookUrl) {
            this.hookUrl = hookUrl;
        } else {
            this.hookUrl = null;
        }
    }

    @DataBoundSetter
    public void setOverrideHookUrl(boolean overrideHookUrl) {
        this.overrideHookUrl = overrideHookUrl;
    }

    public URL getHookUrl() throws GHPluginConfigException {
        try {
            return hookUrl != null
                    ? hookUrl
                    : new URL(Jenkins.getInstance().getRootUrl() + GitHubWebHook.get().getUrlName() + '/');
        } catch (MalformedURLException e) {
            throw new GHPluginConfigException(
                    "Mailformed GH hook url in global configuration (%s)", e.getMessage()
            );
        }
    }

    public boolean isOverrideHookURL() {
        return hookUrl != null;
    }

    /**
     * Filters all stored configs against given predicate then
     * logs in as the given user and returns the non null connection objects
     */
    public Iterable<GitHub> findGithubConfig(Predicate<GitHubServerConfig> match) {
        // try all the credentials since we don't know which one would work
        return from(getConfigs())
                .filter(match)
                .transform(loginToGithub())
                .filter(Predicates.notNull());
    }

    public List<Descriptor> actions() {
        return Collections.singletonList(Jenkins.getInstance().getDescriptor(GitHubTokenCredentialsCreator.class));
    }

    @Extension
    public static class GitHubPluginConfigDescriptor extends Descriptor<GitHubPluginConfig> {

        /**
         * Used to get current instance identity. It compared with same value when testing hook url availability
         */
        @Inject
        @SuppressWarnings("unused")
        private transient InstanceIdentity identity;

        @Override
        public String getDisplayName() {
            return "GitHub Plugin Configuration";
        }

        @SuppressWarnings("unused")
        public FormValidation doReRegister() {
            if (!GitHubPlugin.configuration().isManageHooks()) {
                return FormValidation.warning("Works only when Jenkins manages hooks (one ore more creds specified)");
            }

            List<AbstractProject> registered = GitHubWebHook.get().reRegisterAllHooks();

            LOGGER.info("Called registerHooks() for {} jobs", registered.size());
            return FormValidation.ok("Called re-register hooks for %s jobs", registered.size());
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckHookUrl(@QueryParameter String value) {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(value).openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty(GitHubWebHook.URL_VALIDATION_HEADER, "true");
                con.connect();
                if (con.getResponseCode() != 200) {
                    return FormValidation.error("Got %d from %s", con.getResponseCode(), value);
                }
                String v = con.getHeaderField(GitHubWebHook.X_INSTANCE_IDENTITY);
                if (v == null) {
                    // people might be running clever apps that's not Jenkins, and that's OK
                    return FormValidation.warning("It doesn't look like %s is talking to any Jenkins. " +
                            "Are you running your own app?", value);
                }
                RSAPublicKey key = identity.getPublic();
                String expected = new String(Base64.encodeBase64(key.getEncoded()));
                if (!expected.equals(v)) {
                    // if it responds but with a different ID, that's more likely wrong than correct
                    return FormValidation.error("%s is connecting to different Jenkins instances", value);
                }

                return FormValidation.ok();
            } catch (IOException e) {
                return FormValidation.error(e, "Failed to test a connection to %s", value);
            }
        }
    }
}

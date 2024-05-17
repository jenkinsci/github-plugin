package org.jenkinsci.plugins.github.config;

import com.cloudbees.jenkins.GitHubWebHook;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.XmlFile;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.Permission;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.jenkinsci.main.modules.instance_identity.InstanceIdentity;
import org.jenkinsci.plugins.github.GitHubPlugin;
import org.jenkinsci.plugins.github.Messages;
import org.jenkinsci.plugins.github.internal.GHPluginConfigException;
import org.jenkinsci.plugins.github.migration.Migrator;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Charsets.UTF_8;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.allowedToManageHooks;
import static org.jenkinsci.plugins.github.config.GitHubServerConfig.loginToGithub;
import static org.jenkinsci.plugins.github.internal.GitHubClientCacheOps.clearRedundantCaches;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;

/**
 * Global configuration to store all GH Plugin settings
 * such as hook managing policy, credentials etc.
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.13.0
 */
@Extension
public class GitHubPluginConfig extends GlobalConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPluginConfig.class);
    public static final String GITHUB_PLUGIN_CONFIGURATION_ID = "github-plugin-configuration";

    /**
     * Helps to avoid null in {@link GitHubPlugin#configuration()}
     */
    public static final GitHubPluginConfig EMPTY_CONFIG =
            new GitHubPluginConfig(Collections.emptyList());

    private List<GitHubServerConfig> configs = new ArrayList<>();
    private URL hookUrl;
    @Deprecated
    private transient HookSecretConfig hookSecretConfig;
    private List<HookSecretConfig> hookSecretConfigs;

    /**
     * Used to get current instance identity.
     * It compared with same value when testing hook url availability in {@link #doCheckHookUrl(String)}
     */
    @Inject
    @SuppressWarnings("unused")
    private transient InstanceIdentity identity;

    public GitHubPluginConfig() {
        getConfigFile().getXStream().alias("github-server-config", GitHubServerConfig.class);
        load();
    }

    public GitHubPluginConfig(List<GitHubServerConfig> configs) {
        this.configs = configs;
    }

    private Object readResolve() {
        if (hookSecretConfig != null) {
            if (Util.fixEmpty(hookSecretConfig.getCredentialsId()) != null) {
                setHookSecretConfig(hookSecretConfig);
            }
            hookSecretConfig = null;
        }
        return this;
    }

    @SuppressWarnings("unused")
    @DataBoundSetter
    public void setConfigs(List<GitHubServerConfig> configs) {
        this.configs = configs;
    }

    public List<GitHubServerConfig> getConfigs() {
        return configs;
    }

    public boolean isManageHooks() {
        return from(getConfigs()).filter(allowedToManageHooks()).first().isPresent();
    }

    @DataBoundSetter
    public void setHookUrl(String hookUrl) {
        if (isEmpty(hookUrl)) {
            this.hookUrl = null;
        } else {
            this.hookUrl = parseHookUrl(hookUrl);
        }
    }

    @DataBoundSetter
    @Deprecated
    public void setOverrideHookUrl(boolean overrideHookUrl) {
    }

    /**
     * @return hook url used as endpoint to search and write auto-managed hooks in GH
     * @throws GHPluginConfigException if default jenkins url is malformed
     */
    public URL getHookUrl() throws GHPluginConfigException {
        if (hookUrl != null) {
            return hookUrl;
        } else {
            return constructDefaultUrl();
        }
    }

    @SuppressWarnings("unused")
    public boolean isOverrideHookUrl() {
        return hookUrl != null;
    }

    @Deprecated
    public boolean isOverrideHookURL() {
        return isOverrideHookUrl();
    }

    /**
     * Filters all stored configs against given predicate then
     * logs in as the given user and returns the non null connection objects
     */
    public Iterable<GitHub> findGithubConfig(Predicate<GitHubServerConfig> match) {
        Function<GitHubServerConfig, GitHub> loginFunction = loginToGithub();
        if (Objects.isNull(loginFunction)) {
            return Collections.emptyList();
        }

        // try all the credentials since we don't know which one would work
        return from(getConfigs())
                .filter(match)
                .transform(loginFunction)
                .filter(Predicates.notNull());
    }

    public List<Descriptor> actions() {
        return Collections.singletonList(Jenkins.getInstance().getDescriptor(GitHubTokenCredentialsCreator.class));
    }

    /**
     * To avoid long class name as id in xml tag name and config file
     */
    @Override
    public String getId() {
        return GITHUB_PLUGIN_CONFIGURATION_ID;
    }

    /**
     * @return config file with global {@link com.thoughtworks.xstream.XStream} instance
     * with enabled aliases in {@link Migrator#enableAliases()}
     */
    @Override
    protected XmlFile getConfigFile() {
        return new XmlFile(Jenkins.XSTREAM2, super.getConfigFile().getFile());
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        hookSecretConfigs = null; // form binding might omit empty lists
        try {
            req.bindJSON(this, json);
            // There is a type mismatch between the getter and the setter, so the bindJSON doesn't save
            // the hookUrl properly
            if (json != null && json.containsKey("hookUrl")) {
                this.setHookUrl(json.getString("hookUrl"));
            }
        } catch (Exception e) {
            LOGGER.debug("Problem while submitting form for GitHub Plugin ({})", e.getMessage(), e);
            LOGGER.trace("GH form data: {}", json.toString());
            throw new FormException(
                    format("Malformed GitHub Plugin configuration (%s)", e.getMessage()), e, "github-configuration");
        }
        save();
        clearRedundantCaches(configs);
        return true;
    }

    @Override
    public String getDisplayName() {
        return "GitHub";
    }

    @SuppressWarnings("unused")
    @RequirePOST
    public FormValidation doReRegister() {
        Jenkins.getActiveInstance().checkPermission(Jenkins.MANAGE);
        if (!GitHubPlugin.configuration().isManageHooks()) {
            return FormValidation.warning("Works only when Jenkins manages hooks (one or more creds specified)");
        }

        List<Item> registered = GitHubWebHook.get().reRegisterAllHooks();

        LOGGER.info("Called registerHooks() for {} items", registered.size());
        return FormValidation.ok("Called re-register hooks for %s items", registered.size());
    }

    @RequirePOST
    @Restricted(DoNotUse.class) // WebOnly
    @SuppressWarnings("unused")
    public FormValidation doCheckHookUrl(@QueryParameter String value) {
        Jenkins.getActiveInstance().checkPermission(Jenkins.MANAGE);
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
                // people might be running clever apps that aren't Jenkins, and that's OK
                return FormValidation.warning("It doesn't look like %s is talking to Jenkins. "
                        + "Are you running your own app?", value);
            }
            RSAPublicKey key = identity.getPublic();
            String expected = new String(Base64.encodeBase64(key.getEncoded()), UTF_8);
            if (!expected.equals(v)) {
                // if it responds but with a different ID, that's more likely wrong than correct
                return FormValidation.error("%s is connecting to different Jenkins instances", value);
            }

            return FormValidation.ok();
        } catch (IOException e) {
            return FormValidation.error(e, "Connection test for %s failed", value);
        }
    }

    /**
     * Used by default in {@link #getHookUrl()}
     *
     * @return url to be used in GH hooks configuration as main endpoint
     * @throws GHPluginConfigException if jenkins root url empty of malformed
     */
    private static URL constructDefaultUrl() {
        String jenkinsUrl = Jenkins.getInstance().getRootUrl();
        validateConfig(isNotEmpty(jenkinsUrl), Messages.global_config_url_is_empty());
        try {
            return new URL(jenkinsUrl + GitHubWebHook.get().getUrlName() + '/');
        } catch (MalformedURLException e) {
            throw new GHPluginConfigException(Messages.global_config_hook_url_is_malformed(e.getMessage()));
        }
    }

    /**
     * Util method just to hide one more if for better readability
     *
     * @param state   to check. If false, then exception will be thrown
     * @param message message to describe exception in case of false state
     *
     * @throws GHPluginConfigException if state is false
     */
    private static void validateConfig(boolean state, String message) {
        if (!state) {
            throw new GHPluginConfigException(message);
        }
    }

    @Deprecated
    public HookSecretConfig getHookSecretConfig() {
        return hookSecretConfigs != null && !hookSecretConfigs.isEmpty()
                ? hookSecretConfigs.get(0)
                : new HookSecretConfig(null);
    }

    @Deprecated
    public void setHookSecretConfig(HookSecretConfig hookSecretConfig) {
        setHookSecretConfigs(hookSecretConfig.getCredentialsId() != null
                ? Collections.singletonList(hookSecretConfig)
                : null);
    }

    public List<HookSecretConfig> getHookSecretConfigs() {
        return hookSecretConfigs != null
                ? Collections.unmodifiableList(new ArrayList<>(hookSecretConfigs))
                : Collections.emptyList();
    }

    @DataBoundSetter
    public void setHookSecretConfigs(List<HookSecretConfig> hookSecretConfigs) {
        this.hookSecretConfigs = hookSecretConfigs != null ? new ArrayList<>(hookSecretConfigs) : null;
    }

    private URL parseHookUrl(String hookUrl) {
        try {
            return new URL(hookUrl);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @NonNull
    @Override
    public Permission getRequiredGlobalConfigPagePermission() {
        return Jenkins.MANAGE;
    }
}

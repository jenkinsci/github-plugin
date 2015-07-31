package org.jenkinsci.plugins.github;

import hudson.Plugin;
import hudson.model.Descriptor.FormException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.migration.Migrator;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;

import static java.lang.String.format;
import static org.apache.commons.lang3.Validate.notNull;

/**
 * Main entry point for this plugin
 * Stores global configuration
 *
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubPlugin extends Plugin {
    private GitHubPluginConfig configuration = new GitHubPluginConfig();

    public GitHubPluginConfig getConfiguration() {
        return configuration;
    }

    /**
     * Launched before plugin starts
     * Adds alias for {@link GitHubPlugin} to simplify resulting xml
     */
    public static void init() {
        Jenkins.XSTREAM2.alias("github-plugin", GitHubPlugin.class);
        Migrator.enableCompatibilityAliases();
    }

    @Override
    public void start() throws Exception {
        init();
        load();
    }

    /**
     * Launches migration after plugin already initialized
     */
    @Override
    public void postInitialize() throws Exception {
        new Migrator().migrate();
    }

    @Override
    public void configure(StaplerRequest req, JSONObject formData) throws IOException, ServletException, FormException {
        try {
            configuration = req.bindJSON(GitHubPluginConfig.class, formData);
        } catch (Exception e) {
            throw new FormException(
                    format("Mailformed GitHub Plugin configuration (%s)", e.getMessage()), e, "github-configuration");
        }
        save();
    }

    @Override
    protected void load() throws IOException {
        super.load();
        if (configuration == null) {
            configuration = new GitHubPluginConfig();
            save();
        }
    }

    /**
     * @return instance of this plugin
     */
    public static GitHubPlugin get() {
        return notNull(Jenkins.getInstance(), "Jenkins is not ready to return instance")
                .getPlugin(GitHubPlugin.class);
    }

    /**
     * Shortcut method for {@link GitHubPlugin#get()#getConfiguration()}.
     *
     * @return configuration of plugin
     */
    public static GitHubPluginConfig configuration() {
        return get().getConfiguration();
    }
}

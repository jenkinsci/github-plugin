package org.jenkinsci.plugins.github;

import hudson.Plugin;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.migration.Migrator;

import javax.annotation.Nonnull;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Main entry point for this plugin
 *
 * Launches migration from old config versions
 * Contains helper method to get global plugin configuration - {@link #configuration()}
 *
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubPlugin extends Plugin {
    /**
     * Launched before plugin starts
     * Adds alias for {@link GitHubPlugin} to simplify resulting xml
     */
    public static void init() {
        Migrator.enableCompatibilityAliases();
        Migrator.enableAliases();
    }

    @Override
    public void start() throws Exception {
        init();
    }

    /**
     * Launches migration after plugin already initialized
     */
    @Override
    public void postInitialize() throws Exception {
        new Migrator().migrate();
    }

    /**
     * Shortcut method for getting instance of {@link GitHubPluginConfig}.
     *
     * @return configuration of plugin
     */
    @Nonnull
    public static GitHubPluginConfig configuration() {
        return defaultIfNull(
                GitHubPluginConfig.all().get(GitHubPluginConfig.class),
                GitHubPluginConfig.EMPTY_CONFIG
        );
    }
}

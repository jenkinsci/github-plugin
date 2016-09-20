package org.jenkinsci.plugins.github;

import hudson.Plugin;
import hudson.init.Initializer;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.migration.Migrator;

import javax.annotation.Nonnull;

import static hudson.init.InitMilestone.PLUGINS_PREPARED;
import static hudson.init.InitMilestone.PLUGINS_STARTED;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Main entry point for this plugin
 * <p>
 * Launches migration from old config versions
 * Contains helper method to get global plugin configuration - {@link #configuration()}
 *
 * @author lanwen (Merkushev Kirill)
 */
public class GitHubPlugin extends Plugin {
    /**
     * Launched before plugin starts
     * Adds alias for {@link GitHubPlugin} to simplify resulting xml.
     * Expected milestone: @Initializer(before = PLUGINS_STARTED)
     * * @see #initializers()
     */
    public static void addXStreamAliases() {
        Migrator.enableCompatibilityAliases();
        Migrator.enableAliases();
    }

    /**
     * Launches migration after plugin already initialized.
     * Expected milestone: @Initializer(after = PLUGINS_PREPARED)
     *
     * @see #initializers()
     */
    public static void runMigrator() throws Exception {
        new Migrator().migrate();
    }

    /**
     * We need ensure that migrator will run after xstream aliases will be added.
     * Unclear how reactor will sort single methods, so bundle in one step.
     */
    @Initializer(after = PLUGINS_PREPARED, before = PLUGINS_STARTED)
    public static void initializers() throws Exception {
        addXStreamAliases();
        runMigrator();
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

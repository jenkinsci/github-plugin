package org.jenkinsci.plugins.github;

import hudson.Plugin;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import org.jenkinsci.plugins.github.config.GitHubPluginConfig;
import org.jenkinsci.plugins.github.migration.Migrator;

import edu.umd.cs.findbugs.annotations.NonNull;

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
     */
    public static void addXStreamAliases() {
        Migrator.enableCompatibilityAliases();
        Migrator.enableAliases();
    }

    /**
     * Launches migration after all extensions have been augmented as we need to ensure that the credentials plugin
     * has been initialized.
     * We need ensure that migrator will run after xstream aliases will be added.
     * @see <a href="https://issues.jenkins-ci.org/browse/JENKINS-36446">JENKINS-36446</a>
     */
    @Initializer(after = InitMilestone.EXTENSIONS_AUGMENTED, before = InitMilestone.JOB_LOADED)
    public static void runMigrator() throws Exception {
        new Migrator().migrate();
    }

    @Override
    public void start() throws Exception {
        addXStreamAliases();
    }

    /**
     * Shortcut method for getting instance of {@link GitHubPluginConfig}.
     *
     * @return configuration of plugin
     */
    @NonNull
    public static GitHubPluginConfig configuration() {
        return defaultIfNull(
                GitHubPluginConfig.all().get(GitHubPluginConfig.class),
                GitHubPluginConfig.EMPTY_CONFIG
        );
    }
}

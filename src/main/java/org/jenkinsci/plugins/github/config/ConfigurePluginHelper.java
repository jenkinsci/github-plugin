package org.jenkinsci.plugins.github.config;

import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * This class is workaround for
 * https://issues.jenkins-ci.org/browse/JENKINS-30242
 * until jira-plugin will release https://github.com/jenkinsci/jira-plugin/pull/60
 *
 * @author lanwen (Merkushev Kirill)
 */
public class ConfigurePluginHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurePluginHelper.class);

    private final GitHubPluginConfig conf;

    public ConfigurePluginHelper(GitHubPluginConfig conf) {
        this.conf = conf;
    }

    public void populate(StaplerRequest req, JSONObject json) {
        conf.setConfigs(req.bindJSONToList(GitHubServerConfig.class, json.get("configs")));

        // Workaround for JENKINS-30242
        conf.setOverrideHookUrl(json.getBoolean("overrideHookUrl"));
        conf.setHookUrl(hookUrlFromJson(json));
    }

    private URL hookUrlFromJson(JSONObject json) {
        try {
            return new URL(json.getString("hookUrl"));
        } catch (MalformedURLException e) {
            LOGGER.debug("Hook url malformed - {}", e.getMessage(), e);
            return null;
        }
    }
}

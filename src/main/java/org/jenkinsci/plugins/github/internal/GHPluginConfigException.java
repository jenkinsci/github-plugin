package org.jenkinsci.plugins.github.internal;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class GHPluginConfigException extends RuntimeException {
    public GHPluginConfigException(String message, Object... args) {
        super(String.format(message, args));
    }
}

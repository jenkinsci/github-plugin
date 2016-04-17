package org.jenkinsci.plugins.github.common;

import hudson.model.Run;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;

/**
 * @author lanwen (Merkushev Kirill)
 */
public interface ErrorHandler {
    boolean handle(Exception e, @Nonnull Run<?, ?> run, @Nonnull TaskListener listener) throws Exception;
}

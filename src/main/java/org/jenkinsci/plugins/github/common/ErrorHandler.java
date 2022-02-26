package org.jenkinsci.plugins.github.common;

import hudson.model.Run;
import hudson.model.TaskListener;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * So you can implement bunch of {@link ErrorHandler}s and log, rethrow, ignore exception.
 * Useful to control own step exceptions
 * (for example {@link org.jenkinsci.plugins.github.status.GitHubCommitStatusSetter})
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public interface ErrorHandler {

    /**
     * Normally should return true if exception is handled and no other handler should do anything.
     * If you will return false, the next error handler should try to handle this exception
     *
     * @param e        exception to handle (log, ignore, process, rethrow)
     * @param run      run object from the step
     * @param listener listener object from the step
     *
     * @return true if exception handled successfully
     * @throws Exception you can rethrow exception of any type
     */
    boolean handle(Exception e, @NonNull Run<?, ?> run, @NonNull TaskListener listener) throws Exception;
}

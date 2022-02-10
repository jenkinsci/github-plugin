package org.jenkinsci.plugins.github.common;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

/**
 * With help of list of other error handlers handles exception.
 * If no one will handle it, exception will be wrapped to {@link ErrorHandlingException}
 * and thrown by the handle method
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public class CombineErrorHandler implements ErrorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CombineErrorHandler.class);

    private List<ErrorHandler> handlers = new ArrayList<>();

    private CombineErrorHandler() {
    }

    /**
     * Static factory to produce new instance of this handler
     *
     * @return new instance
     */
    public static CombineErrorHandler errorHandling() {
        return new CombineErrorHandler();
    }

    public CombineErrorHandler withHandlers(List<? extends ErrorHandler> handlers) {
        if (isNotEmpty(handlers)) {
            this.handlers.addAll(handlers);
        }
        return this;
    }

    /**
     * Handles exception with help of other handlers. If no one will handle it, it will be thrown to the top level
     *
     * @param e        exception to handle (log, ignore, process, rethrow)
     * @param run      run object from the step
     * @param listener listener object from the step
     *
     * @return true if exception handled or rethrows it
     */
    @Override
    public boolean handle(Exception e, @NonNull Run<?, ?> run, @NonNull TaskListener listener) {
        LOG.debug("Exception in {} will be processed with {} handlers",
                run.getParent().getName(), handlers.size(), e);
        try {
            for (ErrorHandler next : handlers) {
                if (next.handle(e, run, listener)) {
                    LOG.debug("Exception in {} [{}] handled by [{}]",
                            run.getParent().getName(),
                            e.getMessage(),
                            next.getClass());
                    return true;
                }
            }
        } catch (Exception unhandled) {
            LOG.error("Exception in {} unhandled", run.getParent().getName(), unhandled);
            throw new ErrorHandlingException(unhandled);
        }

        throw new ErrorHandlingException(e);
    }

    /**
     * Wrapper for the not handled by this handler exceptions
     */
    public static class ErrorHandlingException extends RuntimeException {
        public ErrorHandlingException(Throwable cause) {
            super(cause);
        }
    }
}

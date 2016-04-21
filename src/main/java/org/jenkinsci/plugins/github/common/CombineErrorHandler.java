package org.jenkinsci.plugins.github.common;

import hudson.model.Run;
import hudson.model.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class CombineErrorHandler implements ErrorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(CombineErrorHandler.class);

    private List<ErrorHandler> handlers = new ArrayList<>();

    private CombineErrorHandler() {
    }

    public static CombineErrorHandler errorHandling() {
        return new CombineErrorHandler();
    }

    public CombineErrorHandler withHandlers(List<? extends ErrorHandler> handlers) {
        if (isNotEmpty(handlers)) {
            this.handlers.addAll(handlers);
        }
        return this;
    }

    @Override
    public boolean handle(Exception e, @Nonnull Run<?, ?> run, @Nonnull TaskListener listener) {
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

    public static class ErrorHandlingException extends RuntimeException {
        public ErrorHandlingException(Throwable cause) {
            super(cause);
        }
    }
}

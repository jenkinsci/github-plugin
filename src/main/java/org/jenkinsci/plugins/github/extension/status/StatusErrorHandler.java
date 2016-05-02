package org.jenkinsci.plugins.github.extension.status;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.common.ErrorHandler;

/**
 * Extension point to provide way of how to react on errors in status setter step
 *
 * @author lanwen (Merkushev Kirill)
 * @since 1.19.0
 */
public abstract class StatusErrorHandler extends AbstractDescribableImpl<StatusErrorHandler>
        implements ErrorHandler, ExtensionPoint {

    /**
     * Used in view
     *
     * @return all of the available error handlers.
     */
    public static DescriptorExtensionList<StatusErrorHandler, Descriptor<StatusErrorHandler>> all() {
        return Jenkins.getInstance().getDescriptorList(StatusErrorHandler.class);
    }
}

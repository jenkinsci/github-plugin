package org.jenkinsci.plugins.github.extension.status;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.common.ErrorHandler;

/**
 * @author lanwen (Merkushev Kirill)
 */
public abstract class StatusErrorHandler extends AbstractDescribableImpl<StatusErrorHandler>
        implements ErrorHandler, ExtensionPoint {

    public static DescriptorExtensionList<StatusErrorHandler, Descriptor<StatusErrorHandler>> all() {
        return Jenkins.getInstance().getDescriptorList(StatusErrorHandler.class);
    }
}

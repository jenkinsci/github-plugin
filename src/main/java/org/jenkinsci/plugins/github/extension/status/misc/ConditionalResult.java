package org.jenkinsci.plugins.github.extension.status.misc;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;

/**
 * @author lanwen (Merkushev Kirill)
 */
public abstract class ConditionalResult extends AbstractDescribableImpl<ConditionalResult> implements ExtensionPoint {

    protected String status;
    protected String message;

    @DataBoundSetter
    public void setStatus(String status) {
        this.status = status;
    }

    @DataBoundSetter
    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public abstract boolean matches(@Nonnull Run<?, ?> run);

    public abstract static class ConditionalResultDescriptor extends Descriptor<ConditionalResult> {

        public static DescriptorExtensionList<ConditionalResult, Descriptor<ConditionalResult>> all() {
            return Jenkins.getInstance().getDescriptorList(ConditionalResult.class);
        }

        public ListBoxModel doFillStatusItems() {
            ListBoxModel items = new ListBoxModel();
            for (GHCommitState status1 : GHCommitState.values()) {
                items.add(status1.name());
            }
            return items;
        }
    } 


}

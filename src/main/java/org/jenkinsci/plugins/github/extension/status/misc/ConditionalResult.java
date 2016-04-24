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
 * This extension point allows to define when and what to send as state and message.
 * It will be used as part of {@link org.jenkinsci.plugins.github.status.sources.ConditionalStatusResultSource}.
 *
 * @author lanwen (Merkushev Kirill)
 * @see org.jenkinsci.plugins.github.status.sources.misc.BetterThanOrEqualBuildResult
 * @since 1.19.0
 */
public abstract class ConditionalResult extends AbstractDescribableImpl<ConditionalResult> implements ExtensionPoint {

    private String state;
    private String message;

    @DataBoundSetter
    public void setState(String state) {
        this.state = state;
    }

    @DataBoundSetter
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return State to set. Will be converted to {@link GHCommitState}
     */
    public String getState() {
        return state;
    }

    /**
     * @return Message to write. Can contain env vars, will be expanded.
     */
    public String getMessage() {
        return message;
    }

    /**
     * If matches, will be used to set state
     *
     * @param run to check against
     *
     * @return true if matches
     */
    public abstract boolean matches(@Nonnull Run<?, ?> run);

    /**
     * Should be extended to and marked as {@link hudson.Extension} to be in list
     */
    public abstract static class ConditionalResultDescriptor extends Descriptor<ConditionalResult> {

        /**
         * Gets all available extensions. Used in view
         *
         * @return all descriptors of conditional results
         */
        public static DescriptorExtensionList<ConditionalResult, Descriptor<ConditionalResult>> all() {
            return Jenkins.getInstance().getDescriptorList(ConditionalResult.class);
        }

        /**
         * @return options to fill state items in view
         */
        public ListBoxModel doFillStateItems() {
            ListBoxModel items = new ListBoxModel();
            for (GHCommitState commitState : GHCommitState.values()) {
                items.add(commitState.name());
            }
            return items;
        }
    }
}

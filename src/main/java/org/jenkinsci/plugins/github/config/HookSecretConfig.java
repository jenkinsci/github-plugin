package org.jenkinsci.plugins.github.config;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nullable;
import java.util.Collections;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.firstOrDefault;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Manages storing/retrieval of the shared secret for the hook.
 */
@XStreamAlias("hook-config")
public class HookSecretConfig extends AbstractDescribableImpl<HookSecretConfig> {

    private String credentialsId;

    @DataBoundConstructor
    public HookSecretConfig(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    private StringCredentials getHookSecretCredentials() {
        if (isEmpty(credentialsId)) {
            return null;
        }

        return firstOrDefault(
                lookupCredentials(StringCredentials.class,
                        Jenkins.getInstance(), ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()),
                withId(credentialsId), null);
    }

    /**
     * Gets the currently used secret being used for payload verification.
     * @return Current secret, null if not set.
     */
    @Nullable
    public Secret getHookSecret() {
        StringCredentials credentials = getHookSecretCredentials();
        if (credentials != null) {
            return credentials.getSecret();
        } else {
            return null;
        }
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<HookSecretConfig> {

        @Override
        public String getDisplayName() {
            return "Hook secret configuration";
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems() {
            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return new ListBoxModel();
            }

            return new StandardListBoxModel()
                    .withEmptySelection()
                    .withAll(lookupCredentials(
                            StringCredentials.class,
                            Jenkins.getInstance(),
                            ACL.SYSTEM,
                            Collections.<DomainRequirement>emptyList())
                    );
        }
    }
}

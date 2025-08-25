package org.jenkinsci.plugins.github.config;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.security.Permission;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.webhook.SignatureAlgorithm;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;

import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Collections;
import org.kohsuke.stapler.QueryParameter;

/**
 * Manages storing/retrieval of the shared secret for the hook.
 */
public class HookSecretConfig extends AbstractDescribableImpl<HookSecretConfig> {

    private String credentialsId;
    private SignatureAlgorithm signatureAlgorithm;

    @DataBoundConstructor
    public HookSecretConfig(String credentialsId) {
        this(credentialsId, null);
    }
    
    /**
     * Constructor with signature algorithm specification.
     *
     * @param credentialsId the credentials ID for the webhook secret
     * @param signatureAlgorithm the signature algorithm to use (defaults to SHA-256)
     * @since 1.45.0
     */
    public HookSecretConfig(String credentialsId, SignatureAlgorithm signatureAlgorithm) {
        this.credentialsId = credentialsId;
        this.signatureAlgorithm = signatureAlgorithm != null ? signatureAlgorithm : SignatureAlgorithm.DEFAULT;
    }

    /**
     * Gets the currently used secret being used for payload verification.
     *
     * @return Current secret, null if not set.
     */
    @Nullable
    public Secret getHookSecret() {
        return GitHubServerConfig.secretFor(credentialsId).orNull();
    }

    public String getCredentialsId() {
        return credentialsId;
    }
    
    /**
     * Gets the signature algorithm to use for webhook validation.
     *
     * @return the configured signature algorithm, defaults to SHA-256
     * @since 1.45.0
     */
    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm != null ? signatureAlgorithm : SignatureAlgorithm.DEFAULT;
    }

    /**
     * @param credentialsId a new ID
     * @deprecated rather treat this field as final and use {@link GitHubPluginConfig#setHookSecretConfigs}
     */
    @Deprecated
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }
    
    /**
     * Ensures backwards compatibility during deserialization.
     * Sets default algorithm to SHA-256 for existing configurations.
     */
    private Object readResolve() {
        if (signatureAlgorithm == null) {
            signatureAlgorithm = SignatureAlgorithm.DEFAULT;
        }
        return this;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<HookSecretConfig> {

        @Override
        public String getDisplayName() {
            return "Hook secret configuration";
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems(@QueryParameter String credentialsId) {
            if (!Jenkins.getInstance().hasPermission(Jenkins.MANAGE)) {
                return new StandardListBoxModel().includeCurrentValue(credentialsId);
            }

            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(
                            ACL.SYSTEM,
                            Jenkins.getInstance(),
                            StringCredentials.class,
                            Collections.<DomainRequirement>emptyList(),
                            CredentialsMatchers.always()
                    );
        }

        @NonNull
        @Override
        public Permission getRequiredGlobalConfigPagePermission() {
            return Jenkins.MANAGE;
        }
    }
}

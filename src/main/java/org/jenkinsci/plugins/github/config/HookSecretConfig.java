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
    public HookSecretConfig(String credentialsId, String signatureAlgorithm) {
        this.credentialsId = credentialsId;
        this.signatureAlgorithm = parseSignatureAlgorithm(signatureAlgorithm);
    }

    /**
     * Legacy constructor for backwards compatibility.
     */
    public HookSecretConfig(String credentialsId) {
        this(credentialsId, null);
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
     * Gets the signature algorithm name for UI binding.
     *
     * @return the algorithm name as string (e.g., "SHA256", "SHA1")
     * @since 1.45.0
     */
    public String getSignatureAlgorithmName() {
        return getSignatureAlgorithm().name();
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

    /**
     * Parses signature algorithm from UI string input.
     */
    private SignatureAlgorithm parseSignatureAlgorithm(String algorithmName) {
        if (algorithmName == null || algorithmName.trim().isEmpty()) {
            return SignatureAlgorithm.DEFAULT;
        }

        try {
            return SignatureAlgorithm.valueOf(algorithmName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Default to SHA-256 for invalid input
            return SignatureAlgorithm.DEFAULT;
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<HookSecretConfig> {

        @Override
        public String getDisplayName() {
            return "Hook secret configuration";
        }

        /**
         * Provides dropdown items for signature algorithm selection.
         */
        public ListBoxModel doFillSignatureAlgorithmItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("SHA-256 (Recommended)", "SHA256");
            items.add("SHA-1 (Legacy)", "SHA1");
            return items;
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

package org.jenkinsci.plugins.github.config;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.firstOrDefault;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

/**
 * Manages storing/retrieval of the shared secret for the hook.
 */
@Extension
public class HookSecretConfig extends Descriptor<HookSecretConfig> implements Describable<HookSecretConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HookSecretConfig.class);
    private static final String SECRET_ID = "HOOK_SECRET";
    private static final String SECRET_DESCRIPTION = "Secret for signing for verification of received payloads";

    public HookSecretConfig() {
        super(HookSecretConfig.class);
    }

    @Override
    public Descriptor<HookSecretConfig> getDescriptor() {
        return this;
    }

    @Override
    public String getDisplayName() {
        return "Manage the hook secret value";
    }

    /**
     * Sets the secret for the GH hook.
     * @param secretText The secret content/key.
     */
    public void storeSecret(final String secretText) {
        final StringCredentialsImpl credentials = new StringCredentialsImpl(
                CredentialsScope.GLOBAL,
                SECRET_ID,
                SECRET_DESCRIPTION,
                Secret.fromString(secretText)
        );

        final Credentials existingCredentials = getHookSecretCredentials();

        ACL.impersonate(ACL.SYSTEM, new Runnable() {
            @Override
            public void run() {
                try {
                    if (existingCredentials == null) {
                        new SystemCredentialsProvider.StoreImpl().addCredentials(
                                Domain.global(),
                                credentials
                        );
                    } else {
                        new SystemCredentialsProvider.StoreImpl().updateCredentials(
                                Domain.global(),
                                existingCredentials,
                                credentials
                        );
                    }

                } catch (IOException e) {
                    LOGGER.error("Unable to store hook secret", e);
                }
            }
        });
    }

    private StringCredentials getHookSecretCredentials() {
        return firstOrDefault(
                lookupCredentials(StringCredentials.class,
                        Jenkins.getInstance(), ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()),
                withId(SECRET_ID), null);
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
}

package com.cloudbees.jenkins;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;

/**
 * Credential to access GitHub.
 *
 * @author Kohsuke Kawaguchi
 */
public class Credential extends AbstractDescribableImpl<Credential> {
    public final String username;
    public final Secret password;

    @DataBoundConstructor
    public Credential(String username, Secret password) {
        this.username = username;
        this.password = password;
    }

    public GitHub login() throws IOException {
        return GitHub.connect(username,null,password.getPlainText());
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Credential> {
        @Override
        public String getDisplayName() {
            return ""; // unused
        }

        public FormValidation doValidate(@QueryParameter String username, @QueryParameter Secret password) throws IOException {
            if (GitHub.connect(username,null,Secret.toString(password)).isCredentialValid())
                return FormValidation.ok("Verified");
            else
                return FormValidation.error("Failed to validate the account");
        }
    }
}

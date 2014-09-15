package com.cloudbees.jenkins;


import java.io.IOException;

/**
 * Credential to access GitHub.
 *
 * @author Kohsuke Kawaguchi
 */
@Deprecated
public class Credential extends GitHubServerConfig {

    public transient String username;
    public transient String oauthAccessToken;

    private Credential(String apiUrl, String credentialId) {
        super(apiUrl, credentialId);
    }


            }

        }
    }
}

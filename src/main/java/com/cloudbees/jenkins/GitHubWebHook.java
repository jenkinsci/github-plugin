package com.cloudbees.jenkins;

import com.cloudbees.jenkins.GitHubPushTrigger.DescriptorImpl;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;
import hudson.util.AdaptedIterator;
import hudson.util.Iterators.FilterIterator;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.jenkinsci.plugins.github.webhook.GHEventHeader;
import org.jenkinsci.plugins.github.webhook.GHEventPayload;
import org.jenkinsci.plugins.github.webhook.RequirePostWithGHHookPayload;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.jenkinsci.plugins.github.extension.GHEventsSubscriber.isInterestedIn;
import static org.jenkinsci.plugins.github.extension.GHEventsSubscriber.processEvent;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;


/**
 * Receives github hook.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class GitHubWebHook implements UnprotectedRootAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubWebHook.class);
    
    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return URLNAME;
    }

    /**
     * Logs in as the given user and returns the connection object.
     */
    public Iterable<GitHub> login(String host, String userName) {
        final List<Credential> l = DescriptorImpl.get().getCredentials();

        // if the username is not an organization, we should have the right user account on file
        for (Credential c : l) {
            if (c.username.equals(userName)) {
                try {
                    return Collections.singleton(c.login());
                } catch (IOException e) {
                    LOGGER.warn("Failed to login with username={}", c.username, e);
                    return Collections.emptyList();
                }
            }
        }

        // otherwise try all the credentials since we don't know which one would work
        return new Iterable<GitHub>() {
            public Iterator<GitHub> iterator() {
                return new FilterIterator<GitHub>(
                        new AdaptedIterator<Credential, GitHub>(l) {
                            protected GitHub adapt(Credential c) {
                                try {
                                    return c.login();
                                } catch (IOException e) {
                                    LOGGER.warn("Failed to login with username={}", c.username, e);
                                    return null;
                                }
                            }
                        }) {
                    protected boolean filter(GitHub g) {
                        return g != null;
                    }
                };
            }
        };
    }

    /**
     * Receives the webhook call
     * 
     * @param event GH event type. Never null
     * @param payload Payload from hook. Never blank
     */
    @SuppressWarnings("unused")
    @RequirePostWithGHHookPayload
    public void doIndex(@GHEventHeader GHEvent event, @GHEventPayload String payload) {
        from(GHEventsSubscriber.all())
                .filter(isInterestedIn(event))
                .transform(processEvent(event, payload)).toList();
    }

    public static final String URLNAME = "github-webhook";

    // headers used for testing the endpoint configuration
    public static final String URL_VALIDATION_HEADER = "X-Jenkins-Validation";
    public static final String X_INSTANCE_IDENTITY = "X-Instance-Identity";


    public static GitHubWebHook get() {
        return Jenkins.getInstance().getExtensionList(RootAction.class).get(GitHubWebHook.class);
    }

    /**
     * Other plugins may be interested in listening for these updates.
     *
     * @since 1.8
     */
    public static abstract class Listener implements ExtensionPoint {

        /**
         * Called when there is a change notification on a specific repository.
         *
         * @param pusherName        the pusher name.
         * @param changedRepository the changed repository.
         *
         * @since 1.8
         */
        public abstract void onPushRepositoryChanged(String pusherName, GitHubRepositoryName changedRepository);
    }

}

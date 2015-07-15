package com.cloudbees.jenkins;

import com.cloudbees.jenkins.GitHubPushTrigger.DescriptorImpl;
import com.google.common.base.Function;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.AbstractProject;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;
import hudson.triggers.Trigger;
import hudson.util.AdaptedIterator;
import hudson.util.Iterators.FilterIterator;
import hudson.util.SequentialExecutionQueue;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.jenkinsci.plugins.github.internal.GHPluginConfigException;
import org.jenkinsci.plugins.github.webhook.GHEventHeader;
import org.jenkinsci.plugins.github.webhook.GHEventPayload;
import org.jenkinsci.plugins.github.webhook.RequirePostWithGHHookPayload;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static hudson.model.Computer.threadPoolForRemoting;
import static org.apache.commons.lang3.Validate.notNull;
import static org.jenkinsci.plugins.github.extension.GHEventsSubscriber.isInterestedIn;
import static org.jenkinsci.plugins.github.extension.GHEventsSubscriber.processEvent;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isAlive;
import static org.jenkinsci.plugins.github.util.JobInfoHelpers.isBuildable;
import static org.jenkinsci.plugins.github.webhook.WebhookManager.forHookUrl;


/**
 * Receives github hook.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class GitHubWebHook implements UnprotectedRootAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubWebHook.class);
    public static final String URLNAME = "github-webhook";

    // headers used for testing the endpoint configuration
    public static final String URL_VALIDATION_HEADER = "X-Jenkins-Validation";
    public static final String X_INSTANCE_IDENTITY = "X-Instance-Identity";

    private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(threadPoolForRemoting);


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
     * If any wants to auto-register hook, then should call this method
     * Example code:
     * {@code GitHubWebHook.get().registerHookFor(job);}
     *
     * @param job not null project to register hook for
     */
    public void registerHookFor(AbstractProject job) {
        reRegisterHookForJob().apply(job);
    }

    /**
     * Calls {@link #registerHookFor(AbstractProject)} for every project which have subscriber
     *
     * @return list of jobs which jenkins tried to register hook
     */
    public List<AbstractProject> reRegisterAllHooks() {
        return from(getJenkinsInstance().getAllItems(AbstractProject.class))
                .filter(isBuildable())
                .filter(isAlive())
                .transform(reRegisterHookForJob()).toList();
    }

    /**
     * Receives the webhook call
     *
     * @param event   GH event type. Never null
     * @param payload Payload from hook. Never blank
     */
    @SuppressWarnings("unused")
    @RequirePostWithGHHookPayload
    public void doIndex(@Nonnull @GHEventHeader GHEvent event, @Nonnull @GHEventPayload String payload) {
        from(GHEventsSubscriber.all())
                .filter(isInterestedIn(event))
                .transform(processEvent(event, payload)).toList();
    }

    private Function<AbstractProject, AbstractProject> reRegisterHookForJob() {
        return new Function<AbstractProject, AbstractProject>() {
            @Override
            public AbstractProject apply(AbstractProject job) {
                LOGGER.debug("Calling registerHooks() for {}", notNull(job, "Job can't be null").getFullName());

                // We should handle wrong url of self defined hook url here in any case with try-catch :(
                URL hookUrl;
                try {
                    hookUrl = Trigger.all().get(GitHubPushTrigger.DescriptorImpl.class).getHookUrl();
                } catch (GHPluginConfigException e) {
                    LOGGER.error("Skip registration of GHHook ({})", e.getMessage());
                    return job;
                }
                Runnable hookRegistrator = forHookUrl(hookUrl).registerFor(job);
                queue.execute(hookRegistrator);

                return job;
            }
        };
    }

    public static GitHubWebHook get() {
        return Jenkins.getInstance().getExtensionList(RootAction.class).get(GitHubWebHook.class);
    }

    @Nonnull
    public static Jenkins getJenkinsInstance() throws IllegalStateException {
        Jenkins instance = Jenkins.getInstance();
        if (instance == null) {
            throw new IllegalStateException("Jenkins has not been started, or was already shut down");
        }
        return instance;
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

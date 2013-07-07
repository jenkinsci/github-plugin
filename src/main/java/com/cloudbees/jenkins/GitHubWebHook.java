package com.cloudbees.jenkins;

import com.cloudbees.jenkins.GitHubPushTrigger.DescriptorImpl;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.RootAction;
import hudson.model.UnprotectedRootAction;
import hudson.security.ACL;
import hudson.triggers.Trigger;
import hudson.util.AdaptedIterator;
import hudson.util.Iterators.FilterIterator;
import net.sf.json.JSONObject;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.logging.Level.*;

/**
 * Receives github hook.
 * 
 * @author Kohsuke Kawaguchi
 */
@Extension
public class GitHubWebHook implements UnprotectedRootAction {
    private static final Pattern REPOSITORY_NAME_PATTERN = Pattern.compile("https?://([^/]+)/([^/]+)/([^/]+)");

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return "github-webhook";
    }

    /**
     * Logs in as the given user and returns the connection object.
     */
    public Iterable<GitHub> login(String host, String userName) {
        if (host.equals("github.com")) {
            final List<Credential> l = DescriptorImpl.get().getCredentials();

            // if the username is not an organization, we should have the right user account on file
            for (Credential c : l) {
                if (c.username.equals(userName))
                    try {
                        return Collections.singleton(c.login());
                    } catch (IOException e) {
                        LOGGER.log(WARNING,"Failed to login with username="+c.username,e);
                        return Collections.emptyList();
                    }
            }

            // otherwise try all the credentials since we don't know which one would work
            return new Iterable<GitHub>() {
                public Iterator<GitHub> iterator() {
                    return new FilterIterator<GitHub>(
                        new AdaptedIterator<Credential,GitHub>(l) {
                            protected GitHub adapt(Credential c) {
                                try {
                                    return c.login();
                                } catch (IOException e) {
                                    LOGGER.log(WARNING,"Failed to login with username="+c.username,e);
                                    return null;
                                }
                            }
                    }) {
                        protected boolean filter(GitHub g) {
                            return g!=null;
                        }
                    };
                }
            };
        } else {
            return Collections.<GitHub> emptyList();
        }
    }

    /*

    {
        "after":"ea50ac0026d6d9c284e04afba1cc95d86dc3d976",
        "before":"501f46e557f8fc5e0fa4c88a7f4597ef597dd1bf",
        "commits":[
            {
                "added":["b"],
                "author":{"email":"kk@kohsuke.org","name":"Kohsuke Kawaguchi","username":"kohsuke"},
                "id":"3c696af1225e63ed531f5656e8f9cc252e4c96a2",
                "message":"another commit",
                "modified":[],
                "removed":[],
                "timestamp":"2010-12-08T14:31:24-08:00",
                "url":"https://github.com/kohsuke/foo/commit/3c696af1225e63ed531f5656e8f9cc252e4c96a2"
            },{
                "added":["d"],
                "author":{"email":"kk@kohsuke.org","name":"Kohsuke Kawaguchi","username":"kohsuke"},
                "id":"ea50ac0026d6d9c284e04afba1cc95d86dc3d976",
                "message":"new commit",
                "modified":[],
                "removed":[],
                "timestamp":"2010-12-08T14:32:11-08:00",
                "url":"https://github.com/kohsuke/foo/commit/ea50ac0026d6d9c284e04afba1cc95d86dc3d976"
            }
        ],
        "compare":"https://github.com/kohsuke/foo/compare/501f46e...ea50ac0",
        "forced":false,
        "pusher":{"email":"kk@kohsuke.org","name":"kohsuke"},
        "ref":"refs/heads/master",
        "repository":{
            "created_at":"2010/12/08 12:44:13 -0800",
            "description":"testing",
            "fork":false,
            "forks":1,
            "has_downloads":true,
            "has_issues":true,
            "has_wiki":true,
            "homepage":"testing",
            "name":"foo",
            "open_issues":0,
            "owner":{"email":"kk@kohsuke.org","name":"kohsuke"},
            "private":false,
            "pushed_at":"2010/12/08 14:32:23 -0800",
            "url":"https://github.com/kohsuke/foo","watchers":1
        }
    }

     */

    
    /**
     * 1 push to 2 branches will result in 2 pushes.
     */
    // XXX probably want (when available in baseline Stapler version): @RequirePOST
    public void doIndex(StaplerRequest req) {
        String payload = req.getParameter("payload");
        if (payload == null) {
            throw new IllegalArgumentException("Not intended to be browsed interactively (must specify payload parameter)");
        }
        processGitHubPayload(payload,GitHubPushTrigger.class);
    }
    
    public void processGitHubPayload(String payload, Class<? extends Trigger<?>> triggerClass) {
        JSONObject o = JSONObject.fromObject(payload);
        String repoUrl = o.getJSONObject("repository").getString("url"); // something like 'https://github.com/kohsuke/foo'
        String pusherName = o.getJSONObject("pusher").getString("name");

        LOGGER.info("Received POST for "+repoUrl);
        LOGGER.fine("Full details of the POST was "+o.toString());
        Matcher matcher = REPOSITORY_NAME_PATTERN.matcher(repoUrl);
        if (matcher.matches()) {
            // run in high privilege to see all the projects anonymous users don't see.
            // this is safe because when we actually schedule a build, it's a build that can
            // happen at some random time anyway.
            Authentication old = SecurityContextHolder.getContext().getAuthentication();
            SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
            try {
                GitHubRepositoryName changedRepository = GitHubRepositoryName.create(repoUrl);
                for (AbstractProject<?,?> job : Hudson.getInstance().getAllItems(AbstractProject.class)) {
                    GitHubTrigger trigger = (GitHubTrigger) job.getTrigger(triggerClass);
                    if (trigger!=null) {
                        LOGGER.fine("Considering to poke "+job.getFullDisplayName());
                        if (GitHubRepositoryName.from(job).contains(changedRepository)) {
                            LOGGER.info("Poked "+job.getFullDisplayName());
                            trigger.onPost(pusherName);
                        } else
                            LOGGER.fine("Skipped "+job.getFullDisplayName()+" because it doesn't have a matching repository.");
                    }
                }
            } finally {
                SecurityContextHolder.getContext().setAuthentication(old);
            }
        } else {
            LOGGER.warning("Malformed repo url "+repoUrl);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(GitHubWebHook.class.getName());

    public static GitHubWebHook get() {
        return Hudson.getInstance().getExtensionList(RootAction.class).get(GitHubWebHook.class);
    }
}

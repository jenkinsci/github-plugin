package com.cloudbees.jenkins;

import com.cloudbees.jenkins.GitHubPushTrigger.DescriptorImpl;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.RootAction;
import hudson.util.AdaptedIterator;
import hudson.util.Iterators.FilterIterator;
import net.sf.json.JSONObject;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import static java.util.logging.Level.*;

/**
 * Receives github hook.
 * 
 * @author Kohsuke Kawaguchi
 */
@Extension
public class GitHubWebHook implements RootAction {
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
    public Iterable<GitHub> login(String userName) {
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
    public void doIndex(StaplerRequest req) {
        JSONObject o = JSONObject.fromObject(req.getParameter("payload"));
        JSONObject repository = o.getJSONObject("repository");
        String repoUrl = repository.getString("url"); // something like 'https://github.com/kohsuke/foo'
        String repoName = repository.getString("name"); // 'foo' portion of the above URL
        String ownerName = repository.getJSONObject("owner").getString("name"); // 'kohsuke' portion of the above URL
        GitHubRepositoryName changedRepository = new GitHubRepositoryName(ownerName,repoName);

        LOGGER.info("Received POST for "+repoUrl);
        LOGGER.fine("Full details of the POST was "+o.toString());

        for (AbstractProject<?,?> job : Hudson.getInstance().getItems(AbstractProject.class)) {
            GitHubPushTrigger trigger = job.getTrigger(GitHubPushTrigger.class);
            if (trigger!=null) {
                LOGGER.fine("Considering to poke "+job.getFullDisplayName());
                if (trigger.getGitHubRepositories().contains(changedRepository))
                    trigger.onPost();
                else
                    LOGGER.fine("Skipped "+job.getFullDisplayName()+" because it doesn't have a matching repository.");
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(GitHubWebHook.class.getName());

    public static GitHubWebHook get() {
        return Hudson.getInstance().getExtensionList(RootAction.class).get(GitHubWebHook.class);
    }

    static {
        // hide "Bad input type: "search", creating a text input" from createElementNS
        Logger.getLogger(com.gargoylesoftware.htmlunit.html.InputElementFactory.class.getName()).setLevel(WARNING);
    }
}

package com.cloudbees.jenkins;

import hudson.util.Secret;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.logging.Level.WARNING;

public class GitHubSCM {

    private static final Pattern[] URL_PATTERNS = {
            Pattern.compile("https://([^/]+):([^/]+)@.*/([^/]+/[^/]+).git")
    };

    private String url;
    private String userName;
    private String password;
    private String repositoryName;


    public String getUrl() {
        return url;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getRepositoryName(){
        return repositoryName;
    }

    private static final Logger LOGGER = Logger.getLogger(GitHubSCM.class.getName());

    private GitHubSCM(String url, String userName, String password, String repositoryName){
        this.url = url;
        this.userName = userName;
        this.password = password;
        this.repositoryName = repositoryName;
    }

    public Iterable<GitHub> login() {
        Credential c = new Credential(userName, Secret.fromString(password), null, null);
        try {
            return Collections.singleton(c.login());
        } catch (IOException e) {
            LOGGER.log(WARNING,"Failed to login with username="+c.username,e);
            return Collections.emptyList();
        }
    }


    public GHRepository getGitHubRepo(){
        Iterator<GitHub> GHIter = login().iterator();
        if(GHIter.hasNext()){
            GitHub gitHub = GHIter.next();
            try {
                return gitHub.getRepository(repositoryName);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING,"Failed to obtain repository ",e);
                return null;
            }
        }
        return null;
    }

    public static GitHubSCM create(final String url){
        for (Pattern p : URL_PATTERNS) {
            Matcher m = p.matcher(url);
            if (m.matches()){
                return new GitHubSCM("https://api.github.com",m.group(1),m.group(2),m.group(3));
            }
        }
        return null;
    }

}

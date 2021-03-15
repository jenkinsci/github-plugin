# Github Plugin

[![codecov](https://codecov.io/gh/jenkinsci/github-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/jenkinsci/github-plugin)
[![License](https://img.shields.io/github/license/jenkinsci/github-plugin.svg)](LICENSE)

This plugin integrates Jenkins with [Github](http://github.com/)
projects.The plugin currently has three major functionalities:

-   Create hyperlinks between your Jenkins projects and GitHub
-   Trigger a job when you push to the repository by groking HTTP POSTs
    from post-receive hook and optionally auto-managing the hook setup.
-   Report build status result back to github as [Commit
    Status](https://github.com/blog/1227-commit-status-api) ([documented
    on
    SO](https://stackoverflow.com/questions/14274293/show-current-state-of-jenkins-build-on-github-repo/26910986#26910986))
-   Base features for other plugins

## Hyperlinks between changes

The Github plugin decorates Jenkins "Changes" pages to create links to
your Github commit and issue pages. It adds a sidebar link that links
back to the Github project page.

![](/docs/images/image-1.png)
![](/docs/images/image-2.png)

When creating a job, specify that is connects to git. Under "Github
project", put in: git@github.com:*Person*/*Project*.git Under "Source
Code Management" select Git, and put in
git@github.com:*Person*/*Project*.git

## GitHub hook trigger for GITScm polling

This feature enables builds after [post-receive hooks in your GitHub
repositories](https://help.github.com/post-receive-hooks/). This trigger
only kicks git-plugin internal polling algo for every incoming event
against matched repo.

> This plugin was previously named as "Build when a change is pushed to GitHub"

## Usage

To be able to use this feature different mode are available : 
* manual mode : the url have to be added manually in each project
* automatic mode : Jenkins register automatically the webhook for every project

### Manual Mode

In this mode, you'll be responsible for registering the hook URLs to
GitHub. Click the
![(question)](/docs/images/help_16.svg)
icon (under Manage Jenkins \> Configure System \> GitHub) to see the URL
in Jenkins that receives the post-commit POSTs — but in general the URL
is of the form `$JENKINS_BASE_URL/github-webhook/` — for example:
`https://ci.example.com/jenkins/github-webhook/`.

Once you have the URL, and have added it as a webhook to the relevant
GitHub repositories, continue to **Step 3**.

### Automatic Mode (Jenkins manages hooks for jobs by itself)

In this mode, Jenkins will automatically add/remove hook URLs to GitHub
based on the project configuration in the background. You'll specify
GitHub OAuth token so that Jenkins can login as you to do this.

**Step 1.** Go to the global configuration and add GitHub Server Config.

![](/docs/images/image-3.png)

**Step 2.1.** Create your personal access token in GitHub.

Plugin can help you to do it with all required scopes. Go to
**Advanced** -\> **Manage Additional GitHub Actions** -\> **Convert
Login and Password to token**

![](/docs/images/image-4.png)

> *Two-Factor Authentication*
> 
> Auto-creating token doesn't work with [GitHub
> 2FA](https://help.github.com/articles/about-two-factor-authentication/)
> 
> You can create **"Secret text"** credentials with token in corresponding
> domain with login and password directly, or from username and password
> credentials.

**Step 2.2.** Select previously created "Secret Text" credentials with
GitHub OAuth token.

*Required scopes for token*

To be able manage hooks your token should have **admin:org\_hook**
scope.

*GitHub Enterprise*

You can also redefine GitHub url by clicking on **Custom GitHub API
URL** checkbox.  
Note that credentials are filtered by entered GH url with help of domain
requirements. So you can create credentials in different domains and see
only credentials that matched by predefined domains.

![](/docs/images/secret-text.png)

**Step 3.** Once that configuration is done, go to the project config of
each job you want triggered automatically and simply check "GitHub hook trigger for GITScm polling" under "Build Triggers". With this, every new
push to the repository automatically triggers a new build.

![](/docs/images/image-5.png)

Note that there's only one URL and it receives all post-receive POSTs
for all your repositories. The server side of this URL is smart enough
to figure out which projects need to be triggered, based on the
submission.

## Security Implications

This plugin requires that you have an HTTP URL reachable from GitHub,
which means it's reachable from the whole internet. So it is implemented
carefully with the possible malicious fake post-receive POSTS in mind.
To cope with this, upon receiving a POST, Jenkins will talk to GitHub to
ensure the push was actually made.

## Jenkins inside a firewall

In case your Jenkins run inside the firewall and not directly reachable
from the internet, this plugin lets you specify an arbitrary endpoint
URL as an override in the automatic mode. The plugin will assume that
you've set up reverse proxy or some other means so that the POST from
GitHub will be routed to the Jenkins.

## Trouble-shooting hooks

If you set this up but build aren't triggered, check the following
things:

-   Click the "admin" button on the GitHub repository in question and
    make sure post-receive hooks are there.
    -   If it's not there, make sure you have proper credential set in
        the Jenkins system config page.
-   Also, [enable
    logging](https://wiki.jenkins.io/display/JENKINS/Logging) for the
    class names
    -   `com.cloudbees.jenkins.GitHubPushTrigger`
    -   `org.jenkinsci.plugins.github.webhook.WebhookManager`
    -   `com.cloudbees.jenkins.GitHubWebHook`  
        and you'll see the log of Jenkins trying to install a
        post-receive hook.
-   Click "Test hook" button from the GitHub UI and see if Jenkins
    receive a payload.

## Using cache to GitHub requests

Each **GitHub Server Config** creates own GitHub client to interact with
api. By default it uses cache (with **20MB** limit) to speedup process
of fetching data and reduce rate-limit consuming. You can change cache
limit value in "Advanced" section of this config item. If you set 0,
then this feature will be disabled for this (and only this) config.

Additional info:

-   This plugin now serves only hooks from github as main feature. Then
    it starts using git-plugin to fetch sources.
-   It works both public and Enterprise GitHub
-   Plugin have some
    [limitations](https://stackoverflow.com/questions/16323749/jenkins-github-plugin-inverse-branches)

## Possible Issues between Jenkins and GitHub

### Windows:

-   In windows, Jenkins will use the the SSH key of the user it is
    running as, which is located in the %USERPROFILE%\\.ssh folder ( on
    XP, that would be C:\\Documents and Settings\\USERNAME\\.ssh, and on
    7 it would be C:\\Users\\USERNAME\\.ssh). Therefore, you need to
    force Jenkins to run as the user that has the SSH key configured. To
    do that, right click on My Computer, and hit "Manage". Click on
    "Services". Go to Jenkins, right click, and select  "Properties".
    Under the "Log On" tab, choose the user Jenkins will run as, and put
    in the username and password (it requires one). Then restart the
    Jenkins service by right clicking on Jenkins (in the services
    window), and hit "Restart".
-   Jenkins does not support passphrases for SSH keys. Therefore, if you
    set one while running the initial Github configuration, rerun it and
    don't set one.

## Pipeline examples

### Setting commit status

This code will set commit status for custom repo with configured context
and message (you can also define same way backref)

```groovy
void setBuildStatus(String message, String state) {
  step([
      $class: "GitHubCommitStatusSetter",
      reposSource: [$class: "ManuallyEnteredRepositorySource", url: "https://github.com/my-org/my-repo"],
      contextSource: [$class: "ManuallyEnteredCommitContextSource", context: "ci/jenkins/build-status"],
      errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
      statusResultSource: [ $class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
  ]);
}

setBuildStatus("Build complete", "SUCCESS");
```

More complex example (can be used with multiply scm sources in pipeline)

```groovy
def getRepoURL() {
  sh "git config --get remote.origin.url > .git/remote-url"
  return readFile(".git/remote-url").trim()
}

def getCommitSha() {
  sh "git rev-parse HEAD > .git/current-commit"
  return readFile(".git/current-commit").trim()
}

def updateGithubCommitStatus(build) {
  // workaround https://issues.jenkins-ci.org/browse/JENKINS-38674
  repoUrl = getRepoURL()
  commitSha = getCommitSha()

  step([
    $class: 'GitHubCommitStatusSetter',
    reposSource: [$class: "ManuallyEnteredRepositorySource", url: repoUrl],
    commitShaSource: [$class: "ManuallyEnteredShaSource", sha: commitSha],
    errorHandlers: [[$class: 'ShallowAnyErrorHandler']],
    statusResultSource: [
      $class: 'ConditionalStatusResultSource',
      results: [
        [$class: 'BetterThanOrEqualBuildResult', result: 'SUCCESS', state: 'SUCCESS', message: build.description],
        [$class: 'BetterThanOrEqualBuildResult', result: 'FAILURE', state: 'FAILURE', message: build.description],
        [$class: 'AnyBuildResult', state: 'FAILURE', message: 'Loophole']
      ]
    ]
  ])
}
```

## Change Log

[GitHub Releases](https://github.com/jenkinsci/github-plugin/releases)

## Development

Start the local Jenkins instance:

    mvn hpi:run


## Jenkins Plugin Maven goals

	hpi:create  Creates a skeleton of a new plugin.
	
	hpi:hpi Builds the .hpi file

	hpi:hpl Generates the .hpl file

	hpi:run Runs Jenkins with the current plugin project

	hpi:upload Posts the hpi file to java.net. Used during the release.
	
	
## How to install

Run 

	mvn hpi:hpi
	
to create the plugin .hpi file.


To install:

1. copy the resulting ./target/rdoc.hpi file to the $JENKINS_HOME/plugins directory. Don't forget to restart Jenkins afterwards.
	
2. or use the plugin management console (https://example.com:8080/pluginManager/advanced) to upload the hpi file. You have to restart Jenkins in order to find the plugin in the installed plugins list.


## Plugin releases

	mvn release:prepare release:perform -Dusername=juretta -Dpassword=******

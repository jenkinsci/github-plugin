package com.cloudbees.jenkins.GitHubCommitNotifier

import com.cloudbees.jenkins.GitHubCommitNotifier

def f = namespace(lib.FormTagLib);

// prepare default instance
if (instance == null) {
    instance = new GitHubCommitNotifier()
}

f.advanced() {
    f.entry(title: _('Build status message'), field: 'statusMessage') {
        f.property()
    }
    
    f.entry(title: _('Result on failure'), field: 'resultOnFailure') {
        f.select()
    }
}

package org.jenkinsci.plugins.github.admin.GitHubHookRegisterProblemMonitor

import hudson.util.VersionNumber
import jenkins.model.Jenkins

def f = namespace(lib.FormTagLib)

if (Jenkins.getVersion().isNewerThan(new VersionNumber("2.88"))) {
    div(class: 'alert alert-warning') {
        form(method: 'post', action: "${rootURL}/${my?.url}/act", name: my?.id) {
            f.submit(name: 'yes', value: _('view'))
            f.submit(name: 'no', value: _('dismiss'))
        }
        text(_('hook.registering.problem'))
    }
} else {
    div(class: 'warning') {
        form(method: 'post', action: "${rootURL}/${my?.url}/act", name: my?.id) {
            text(_('hook.registering.problem'))
            f.submit(name: 'yes', value: _('view'))
            f.submit(name: 'no', value: _('dismiss'))
        }
    }
}
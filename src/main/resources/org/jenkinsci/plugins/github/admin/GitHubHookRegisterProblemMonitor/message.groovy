package org.jenkinsci.plugins.github.admin.GitHubHookRegisterProblemMonitor

def f = namespace(lib.FormTagLib)

div(class: 'alert alert-warning') {
    form(method: 'post', action: "${rootURL}/${my?.url}/act", name: my?.id) {
        f.submit(name: 'yes', value: _('view'))
        f.submit(name: 'no', value: _('dismiss'))
    }
    text(_('hook.registering.problem'))
}

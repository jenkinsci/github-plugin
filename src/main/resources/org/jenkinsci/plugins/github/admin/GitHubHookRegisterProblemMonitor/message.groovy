package org.jenkinsci.plugins.github.admin.GitHubHookRegisterProblemMonitor

def f = namespace(lib.FormTagLib)

div(class: 'warning') {
    form(method: 'post', action: "${rootURL}/${my?.url}/act", name: my?.id) {
        text(_('hook.registering.problem'))
        f.submit(name: 'yes', value: _('view'))
        f.submit(name: 'no', value: _('dismiss'))
    }
}

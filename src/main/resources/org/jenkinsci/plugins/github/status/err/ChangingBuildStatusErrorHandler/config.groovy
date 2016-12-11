package org.jenkinsci.plugins.github.status.err.ChangingBuildStatusErrorHandler

def f = namespace(lib.FormTagLib);

f.entry(title: _('Result on failure'), field: 'result') {
    f.select()
}

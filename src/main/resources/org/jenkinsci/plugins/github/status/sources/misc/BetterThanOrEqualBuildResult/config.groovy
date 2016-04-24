package org.jenkinsci.plugins.github.status.sources.misc.BetterThanOrEqualBuildResult


def f = namespace(lib.FormTagLib);


f.entry(title: _('Build result better than or equal to'), field: 'result') {
    f.select()
}

f.entry(title: _('Status'), field: 'state') {
    f.select()
}

f.entry(title: _('Message'), field: 'message') {
    f.textbox()
}

package org.jenkinsci.plugins.github.status.sources.ManuallyEnteredBackrefSource


def f = namespace(lib.FormTagLib);

f.entry(title: _('Backref URL'), field: 'backref') {
    f.textbox()
}

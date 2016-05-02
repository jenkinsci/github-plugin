package org.jenkinsci.plugins.github.status.sources.ManuallyEnteredCommitContextSource


def f = namespace(lib.FormTagLib);

f.entry(title: _('Context name'), field: 'context') {
    f.textbox()
}

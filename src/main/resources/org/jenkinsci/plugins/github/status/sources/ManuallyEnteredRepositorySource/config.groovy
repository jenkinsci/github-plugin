package org.jenkinsci.plugins.github.status.sources.AnyDefinedRepositorySource


def f = namespace(lib.FormTagLib);

f.entry(title: _('Repository URL'), field: 'url') {
    f.textbox()
}
package org.jenkinsci.plugins.github.status.sources.ManuallyEnteredShaSource


def f = namespace(lib.FormTagLib);

f.entry(title: _('SHA'), field: 'sha') {
    f.textbox()
}

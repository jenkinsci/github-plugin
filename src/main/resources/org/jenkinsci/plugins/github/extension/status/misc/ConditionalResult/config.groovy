package org.jenkinsci.plugins.github.extension.status.misc.ConditionalResult


def f = namespace(lib.FormTagLib);

f.entry(title: _('Status'), field: 'state') {
    f.select()
}

f.entry(title: _('Message'), field: 'message') {
    f.textbox()
}

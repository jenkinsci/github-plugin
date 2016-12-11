package org.jenkinsci.plugins.github.common.ExpandableMessage

def f = namespace(lib.FormTagLib);

f.entry(title: _('Content'), field: 'content') {
    f.expandableTextbox()
}

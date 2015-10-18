package org.jenkinsci.plugins.github.common.ExpandableMessage

def f = namespace(lib.FormTagLib);

f.entry(title: _('Message content')) {
    f.expandableTextbox(field: 'content')
}

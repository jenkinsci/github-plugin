package org.jenkinsci.plugins.github.config.HookSecretConfig

def f = namespace(lib.FormTagLib);
def c = namespace(lib.CredentialsTagLib);

f.entry(title: _("Shared secret"), field: "credentialsId", help: descriptor.getHelpFile('sharedSecret')) {
    c.select(context: app, includeUser: false, expressionAllowed: false)
}

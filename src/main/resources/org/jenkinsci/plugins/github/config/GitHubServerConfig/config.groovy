package org.jenkinsci.plugins.github.config.GitHubServerConfig

import org.jenkinsci.plugins.github.config.GitHubServerConfig

def f = namespace(lib.FormTagLib);
def c = namespace(lib.CredentialsTagLib)

f.entry(title: _("Name"), field: "name") {
    f.textbox()
}

f.entry(title: _("API URL"), field: "apiUrl") {
    f.textbox(default: GitHubServerConfig.GITHUB_URL)
}

f.entry(title: _("Credentials"), field: "credentialsId") {
    c.select(context:app, includeUser:false, expressionAllowed:false)
}

f.block() {
    f.validateButton(
            title: _("Test connection"),
            progress: _("Testing..."),
            method: "verifyCredentials",
            with: "apiUrl,credentialsId"
    )
}


f.entry(title: _("Manage hooks"), field: "manageHooks") {
    f.checkbox(default: true)
}

f.advanced() {
    f.entry(title: _("GitHub client cache size (MB)"), field: "clientCacheSize") {
        f.textbox(default: GitHubServerConfig.DEFAULT_CLIENT_CACHE_SIZE_MB)
    }
}

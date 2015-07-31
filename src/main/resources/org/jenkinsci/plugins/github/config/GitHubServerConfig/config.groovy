package org.jenkinsci.plugins.github.config.GitHubServerConfig

import org.jenkinsci.plugins.github.config.GitHubServerConfig

def f = namespace(lib.FormTagLib);
def c = namespace(lib.CredentialsTagLib)


f.entry(title: _("Manage hooks"), field: "manageHooks") {
    f.checkbox(default: true)
}

f.entry(title: _("Credentials"), field: "credentialsId") {
    c.select()
}

f.optionalBlock(title: _("Custom GitHub API URL"), 
        inline: true, 
        field: "custom", 
        checked: instance?.custom) {
    f.entry(title: _("GitHub API URL"), field: "apiUrl") {
        f.textbox(default: GitHubServerConfig.GITHUB_URL)
    }
}

f.block() {
    f.validateButton(
            title: _("Verify credentials"),
            progress: _("Verifying..."),
            method: "verifyCredentials",
            with: "apiUrl,credentialsId"
    )
}

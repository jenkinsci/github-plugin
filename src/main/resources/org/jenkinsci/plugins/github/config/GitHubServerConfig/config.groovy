package org.jenkinsci.plugins.github.config.GitHubServerConfig

import org.jenkinsci.plugins.github.config.GitHubServerConfig

def f = namespace(lib.FormTagLib);
def c = namespace(lib.CredentialsTagLib)


f.entry(title: _("Don't manage hooks with this config")) {
    f.checkbox( field: "dontUseItToMangeHooks")
}

f.entry(title: _("Credentials"), field: "credentialsId") {
    c.select()
}

f.optionalBlock(title: _("Custom GitHub API URL"), inline: true, name: "custom", checked: instance?.custom) {
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

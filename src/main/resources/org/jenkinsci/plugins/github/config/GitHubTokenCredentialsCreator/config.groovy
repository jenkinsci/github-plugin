package org.jenkinsci.plugins.github.config.GitHubTokenCredentialsCreator

import org.jenkinsci.plugins.github.config.GitHubServerConfig

def f = namespace(lib.FormTagLib);
def c = namespace(lib.CredentialsTagLib)

f.entry(title: _("GitHub API URL"), field: "apiUrl") {
    f.textbox(default: GitHubServerConfig.GITHUB_URL)
}

f.radioBlock(checked: true, name: "creds", value: "plugin", title: "From credentials") {
    f.entry(title: _("Credentials"), field: "credentialsId") {
        c.select()
    }

    f.block() {
        f.validateButton(
                title: _("Create token credentials"),
                progress: _("Creating..."),
                method: "createTokenByCredentials",
                with: "apiUrl,credentialsId"
        )
    }
}

f.radioBlock(checked: false, name: "creds", value: "manually", title: "From login and password") {

    f.entry(title: _("Login"), field: "login") {
        f.textbox()
    }

    f.entry(title: _("Password"), field: "password") {
        f.password()
    }

    f.block() {
        f.validateButton(
                title: _("Create token credentials"),
                progress: _("Creating..."),
                method: "createTokenByPassword",
                with: "apiUrl,login,password"
        )
    }
}



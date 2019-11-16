package com.cloudbees.jenkins.GitHubPushTrigger

import com.cloudbees.jenkins.GitHubPushTrigger

def f = namespace(lib.FormTagLib);

tr {
    td(colspan: 4) {
        div(id: 'gh-hooks-warn')
    }
}

script(src:"${rootURL}${h.getResourcePath()}/plugin/github/js/warning.js")
script {
    text("""
InlineWarning.setup({ 
    id: 'gh-hooks-warn',
    url: ${descriptor.getCheckMethod('hookRegistered').toCheckUrl()}, 
    input: 'input[name="${GitHubPushTrigger.class.getName().replace(".", "-")}"]'
}).start();
""")
}

f.entry() {
    f.checkbox(title: _("Use Git excluded user list (\"Polling ignores commits from certain users\")"), field: "useGitExcludedUsers")
}

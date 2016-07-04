package com.cloudbees.jenkins.GitHubPushTrigger

import com.cloudbees.jenkins.GitHubPushTrigger

def f = namespace(lib.FormTagLib);

f.entry(field: "sharedSecret", title: "Shared secret", help: descriptor.getHelpFile('sharedSecret')) {
    f.password()
}

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

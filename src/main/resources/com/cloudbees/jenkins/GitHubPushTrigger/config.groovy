package com.cloudbees.jenkins.GitHubPushTrigger

import com.cloudbees.jenkins.GitHubPushTrigger

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

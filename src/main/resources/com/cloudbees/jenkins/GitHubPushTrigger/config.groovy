package com.cloudbees.jenkins.GitHubPushTrigger

import com.cloudbees.jenkins.GitHubPushTrigger

tr {
    td(colspan: 4) {
        def url = descriptor.getCheckMethod('hookRegistered').toCheckUrl()
        def input = "input[name='${GitHubPushTrigger.class.getName().replace('.', '-')}']"

        div(id: 'gh-hooks-warn',
                'data-url': url,
                'data-input': input
        )
    }
}

script(src:"${rootURL}${h.getResourcePath()}/plugin/github/js/warning.js")

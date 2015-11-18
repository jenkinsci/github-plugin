package com.coravy.hudson.plugins.github.GithubProjectProperty

import static com.coravy.hudson.plugins.github.GithubProjectProperty.DescriptorImpl.GITHUB_PROJECT_BLOCK_NAME

def f = namespace(lib.FormTagLib);

f.optionalBlock(name: GITHUB_PROJECT_BLOCK_NAME, title: _('github.project'), checked: instance != null) {
    f.entry(field: 'projectUrlStr', title: _('github.project.url')) {
        f.textbox()
    }

    f.advanced() {
        f.entry(title: _('github.build.status.context.for.commits'), field: 'statusContext') {
            f.textbox()
        }
    }
}

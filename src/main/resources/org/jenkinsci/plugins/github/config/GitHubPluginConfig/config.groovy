package org.jenkinsci.plugins.github.config.GitHubPluginConfig

import com.cloudbees.jenkins.GitHubPushTrigger

def f = namespace(lib.FormTagLib);

f.section(title: descriptor.displayName) {
    f.entry(title: _("Servers configs with credentials to manage GitHub integrations"),
            description: _("List of GitHub Servers to manage hooks, set commit statuses etc."),
            help: descriptor.getHelpFile()) {
        
        f.repeatableHeteroProperty(
                field: "configs",
                hasHeader: "true",
                addCaption: _("Add GitHub Server Config"),
                deleteCaption: _("Delete config"))
    }

    if (instance.manageHooks) {
        f.validateButton(
                title: _("Re-register hooks for all jobs"),
                progress: _("Scanning all items..."),
                method: "reRegister"
        )
    }

    f.advanced() {
        if (GitHubPushTrigger.ALLOW_HOOKURL_OVERRIDE) {
            f.entry(title: _("Override Hook URL")) {
                table(width: "100%", style: "margin-left: 7px;") {
                    f.optionalBlock(title: _("Specify another hook url for GitHub configuration"),
                            inline: true,
                            field: "overrideHookUrl",
                            checked: instance.overrideHookURL) {
                        f.entry(field: "hookUrl") {
                            f.textbox()
                        }
                    }
                }
            }
        }

        f.entry(title: _("Additional actions"), help: descriptor.getHelpFile('additional')) {
            f.hetero_list(items: [],
                    addCaption: _("Manage additional GitHub actions"),
                    name: "actions",
                    oneEach: "true", hasHeader: "true", descriptors: instance.actions())
        }
    }
}

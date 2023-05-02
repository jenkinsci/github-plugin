package org.jenkinsci.plugins.github.config.GitHubPluginConfig

import com.cloudbees.jenkins.GitHubPushTrigger
import lib.FormTagLib

def f = namespace(FormTagLib);
def g = namespace("/lib/github")

f.section(title: descriptor.displayName) {
    f.entry(title: _("GitHub Servers"),
            help: descriptor.getHelpFile()) {
        
        f.repeatableHeteroProperty(
                field: "configs",
                hasHeader: "true",
                addCaption: _("Add GitHub Server"))
    }

    f.advanced() {
        f.validateButton(
                title: _("Re-register hooks for all jobs"),
                progress: _("Scanning all items..."),
                method: "reRegister"
        )
        
        if (GitHubPushTrigger.ALLOW_HOOKURL_OVERRIDE) {
            f.entry(title: _("Override Hook URL")) {
                g.blockWrapper {
                    f.optionalBlock(title: _("Specify another hook URL for GitHub configuration"),
                            name: "isOverrideHookUrl",
                            inline: true,
                            checked: instance.isOverrideHookUrl()) {
                        f.entry(field: "hookUrl") {
                            f.textbox(checkMethod: "post", name: "hookUrl")
                        }
                    }
                }
            }
        }

        f.entry(title: _("Shared secrets")) {
            f.repeatableProperty(
                field: "hookSecretConfigs",
                add: _("Add shared secret")
            ) {
                f.entry(title: "") {
                    div(align: "right") {
                        f.repeatableDeleteButton()
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

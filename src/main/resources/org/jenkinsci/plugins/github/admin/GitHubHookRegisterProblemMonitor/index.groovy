package org.jenkinsci.plugins.github.admin.GitHubHookRegisterProblemMonitor

import com.cloudbees.jenkins.GitHubWebHook
import org.jenkinsci.plugins.github.admin.GitHubHookRegisterProblemMonitor
import org.jenkinsci.plugins.github.webhook.WebhookManager

def f = namespace(lib.FormTagLib);
def st = namespace('jelly:stapler')
def l = namespace(lib.LayoutTagLib)

l.layout(title: _('page.title'), permission: app.ADMINISTER) {
    l.header() {
        link(rel: 'stylesheet', type: 'text/css', href: "${rootURL}${h.getResourcePath()}/plugin/github/css/monitor.css")
    }
    st.include(page: 'sidepanel.jelly', it: app)
    l.main_panel {
        div(class: 'gh-page') {

            h1 {
                text(_('page.title'))
            }

            div {
                p {
                    text(_('help.for.page.and.debug.info'))
                }

                ul {
                    [
                            GitHubWebHook.class.getName(),
                            WebhookManager.class.getName(),
                            GitHubHookRegisterProblemMonitor.class.getName()
                    ].each { classname ->
                        li {
                            text("${classname} - ")
                            b {
                                text('ALL')
                            }
                        }
                    }
                }
            }

            if (!my.problems.isEmpty()) {
                div {
                    p {
                        text(_('help.for.problems'))
                    }
                }
                table(class: 'pane bigtable', style: 'width:auto') {
                    tr(class: 'repo-table__header') {
                        th {
                            text(_('project.header'))
                        }
                        th {
                            text(_('message.header'))
                        }
                        th {
                            text('')
                        }
                    }

                    my.problems.entrySet().each { entry ->
                        tr(class: 'repo-line') {
                            td(class: 'repo-line__title') {
                                text("${entry.key.host}:${entry.key.userName}/${entry.key.repositoryName}")
                            }
                            td(class: 'repo-line__msg') {
                                text(entry.value)
                            }
                            td {
                                f.form(method: 'post', action: "${rootURL}/${my?.url}/ignore", name: 'ignore') {
                                    f.invisibleEntry {
                                        f.textbox(name: 'repo', value: "https://${entry.key.host}/${entry.key.userName}/${entry.key.repositoryName}")
                                    }
                                    f.submit(name: 'yes', value: _('ignore'))
                                }
                            }
                        }
                    }
                }
                br()
                br()
            }

            if (!my.ignored.isEmpty()) {
                div {
                    p {
                        text(_('help.for.ignored'))
                    }
                }
                table(class: 'pane bigtable', style: 'width:auto') {
                    tr(class: 'repo-table__header') {
                        th {
                            text(_('ignored.projects'))
                        }
                        th {
                            text('')
                        }
                    }

                    my.ignored.each { entry ->
                        tr(class: 'repo-line') {
                            td(class: 'repo-line__title') {
                                text("${entry.host}:${entry.userName}/${entry.repositoryName}")
                            }
                            td {
                                f.form(method: 'post', action: "${rootURL}/${my?.url}/disignore", name: 'disignore') {
                                    f.invisibleEntry {
                                        f.textbox(name: 'repo', value: "https://${entry.host}/${entry.userName}/${entry.repositoryName}")
                                    }
                                    f.submit(name: 'yes', value: _('disignore'))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

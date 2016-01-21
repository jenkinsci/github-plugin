package org.jenkinsci.plugins.github.admin.GitHubHookRegisterProblemMonitor

def f = namespace(lib.FormTagLib);
def st = namespace('jelly:stapler')
def l = namespace(lib.LayoutTagLib)

l.layout(title: _('page.title'), permission: app.ADMINISTER) {
    st.include(page: 'sidepanel.jelly', it: app)
    l.main_panel {
        h1 {
            text(_('page.title'))
        }

        if (!my.problems.isEmpty()) {
            table(class: 'pane bigtable', style: 'width:auto') {
                tr {
                    th {
                        text(_('Project with hook problem'))
                    }
                    th {
                        text(_('Message'))
                    }
                    th {
                        text('')
                    }
                }

                my.problems.entrySet().each { entry ->
                    tr {
                        td {
                            text("${entry.key.host}:${entry.key.userName}/${entry.key.repositoryName}")
                        }
                        td {
                            text(entry.value?.message)
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
            br()
        }
       
        if (!my.ignored.isEmpty()) {
            table(class: 'pane bigtable', style: 'width:auto') {
                tr {
                    th {
                        text(_('Ignored Projects'))
                    }
                    th {
                        text('')
                    }
                }

                my.ignored.each { entry ->
                    tr {
                        td {
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

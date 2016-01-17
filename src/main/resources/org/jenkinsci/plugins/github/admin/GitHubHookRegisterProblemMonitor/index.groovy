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
        table(class: 'pane bigtable', style: 'width:auto') {
            tr {
                th {
                    text(_('project'))
                }
                th {
                    text(_('message'))
                }
            }

            my.problems.entrySet().each { entry ->
                tr {
                    td {
                        text(entry.key)
                    }
                    td {
                        text(entry.value?.message)
                    }
                }
            }
        }
    }
}

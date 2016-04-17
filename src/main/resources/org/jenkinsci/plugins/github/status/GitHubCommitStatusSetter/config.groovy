package org.jenkinsci.plugins.github.status.GitHubCommitStatusSetter

import org.apache.commons.collections.CollectionUtils
import org.jenkinsci.plugins.github.extension.status.StatusErrorHandler


def f = namespace(lib.FormTagLib);

f.section(title: _('Where:')) {
    f.dropdownDescriptorSelector(title: _('Commit SHA: '), field: 'commitShaSource')
    f.dropdownDescriptorSelector(title: _('Repositories: '), field: 'reposSource')
}

f.section(title: _('What:')) {
    f.dropdownDescriptorSelector(title: _('Commit context: '), field: 'contextSource')
    f.dropdownDescriptorSelector(title: _('Status result: '), field: 'statusResultSource')
}

f.advanced {
    f.section(title: _('Advanced:')) {
        f.optionalBlock(
                checked: CollectionUtils.isNotEmpty(instance?.errorHandlers),
                inline: true,
                name: 'errorHandling',
                title: 'Handle errors') {
            f.block {
                f.hetero_list(items: CollectionUtils.isEmpty(instance?.errorHandlers)
                        ? []
                        : instance.errorHandlers,
                        addCaption: 'Add handler',
                        name: 'errorHandlers',
                        oneEach: true, hasHeader: true, descriptors: StatusErrorHandler.all())
            }
        }
    }
}

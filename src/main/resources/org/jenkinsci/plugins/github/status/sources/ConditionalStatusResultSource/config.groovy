package org.jenkinsci.plugins.github.status.sources.ConditionalStatusResultSource

import org.apache.commons.collections.CollectionUtils
import org.jenkinsci.plugins.github.extension.status.misc.ConditionalResult.ConditionalResultDescriptor;

def f = namespace(lib.FormTagLib);

f.helpLink(url: descriptor.getHelpFile())
f.helpArea()

f.block {
    f.hetero_list(items: CollectionUtils.isEmpty(instance?.results)
            ? []
            : instance.results,
            addCaption: 'If Run',
            name: 'results',
            oneEach: false, hasHeader: true, descriptors: ConditionalResultDescriptor.all())
}

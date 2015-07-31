package org.jenkinsci.plugins.github.GitHubPlugin

def st = namespace("jelly:stapler");

set("instance", my.configuration);
set("descriptor", instance.descriptor);
st.include(from: descriptor, page: descriptor.configPage, optional: false)

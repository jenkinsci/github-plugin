package org.jenkinsci.plugins.github.GitHubPlugin

def st = namespace("jelly:stapler");

instance = my.configuration
descriptor = instance.descriptor
st.include(from: descriptor, page: descriptor.configPage, optional: false)

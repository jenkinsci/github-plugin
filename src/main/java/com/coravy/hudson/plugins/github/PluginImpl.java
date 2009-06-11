/*
 * $Id: PluginImpl.java 18781 2009-06-11 00:54:46Z juretta $ 
 */
package com.coravy.hudson.plugins.github;

import hudson.Plugin;

/**
 * 
 * @author Stefan Saasen <stefan@coravy.com>
 * @plugin
 */
public class PluginImpl extends Plugin {

    /* (non-Javadoc)
     * @see hudson.Plugin#postInitialize()
     */
    @Override
    public void postInitialize() throws Exception {
        
    }

    /*
     * (non-Javadoc)
     * @see hudson.Plugin#start()
     */
    @Override
    public void start() throws Exception {
        //Jobs.PROPERTIES.add(GithubProjectProperty.DESCRIPTOR);
    }

    /*
     * (non-Javadoc)
     * @see hudson.Plugin#stop()
     */
    @Override
    public void stop() throws Exception {
        super.stop();
    }

}

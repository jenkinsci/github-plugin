package org.jenkinsci.plugins.github.extension.status;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * Extension point to provide backref for the status, i.e. to the build or to the test report.
 *
 * @author pupssman (Kalinin Ivan)
 * @since 1.21.2
 */
public abstract class GitHubStatusBackrefSource extends AbstractDescribableImpl<GitHubStatusBackrefSource>
        implements ExtensionPoint {

    /**
     * @param run      actual run
     * @param listener build listener
     *
     * @return URL that points to the status source, i.e. test result page
     */
    public abstract String get(Run<?, ?> run, TaskListener listener);

}

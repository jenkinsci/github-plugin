package org.jenkinsci.plugins.github.test;

import hudson.LocalPluginManager;
import org.jvnet.hudson.test.JenkinsRecipe;
import org.jvnet.hudson.test.JenkinsRule;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author lanwen (Merkushev Kirill)
 */
@Documented
@JenkinsRecipe(WithoutPlugins.RuleRunnerImpl.class)
@Target(METHOD)
@Retention(RUNTIME)
public @interface WithoutPlugins {
    class RuleRunnerImpl extends JenkinsRecipe.Runner<WithoutPlugins> {
        
        @Override
        public void setup(JenkinsRule jenkinsRule, WithoutPlugins recipe) throws Exception {
            jenkinsRule.setPluginManager(new LocalPluginManager(jenkinsRule.getWebAppRoot()));
        }
    }
}

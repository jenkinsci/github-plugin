package org.jenkinsci.plugins.github.admin;

import com.cloudbees.jenkins.GitHubRepositoryName;
import org.kohsuke.stapler.AnnotationHandler;
import org.kohsuke.stapler.InjectedParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;

import javax.servlet.ServletException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.apache.commons.lang3.Validate.notNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * InjectedParameter annotation to use on WebMethod parameters.
 * Handles converts form submission to {@link GitHubRepositoryName}
 *
 * @author lanwen (Merkushev Kirill)
 * @see <a href=https://wiki.jenkins-ci.org/display/JENKINS/Web+Method>Web Method</a>
 */
@Retention(RUNTIME)
@Target(PARAMETER)
@Documented
@InjectedParameter(GHRepoName.PayloadHandler.class)
public @interface GHRepoName {
    class PayloadHandler extends AnnotationHandler<GHRepoName> {
        private static final Logger LOGGER = getLogger(PayloadHandler.class);

        /**
         * @param param name of param in form and name of the argument in web-method
         *
         * @return {@link GitHubRepositoryName} extracted from request or null on any problem
         */
        @Override
        public Object parse(StaplerRequest req, GHRepoName a, Class type, String param) throws ServletException {
            String repo = notNull(req, "Why StaplerRequest is null?").getParameter(param);
            LOGGER.trace("Repo url in method {}", repo);
            return GitHubRepositoryName.create(repo);
        }
    }
}

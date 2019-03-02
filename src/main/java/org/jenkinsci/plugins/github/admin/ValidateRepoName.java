package org.jenkinsci.plugins.github.admin;

import com.cloudbees.jenkins.GitHubRepositoryName;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.Interceptor;
import org.kohsuke.stapler.interceptor.InterceptorAnnotation;

import javax.servlet.ServletException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.jenkinsci.plugins.github.util.FluentIterableWrapper.from;
import static org.kohsuke.stapler.HttpResponses.errorWithoutStack;

/**
 * InterceptorAnnotation annotation to use on WebMethod signature.
 * Encapsulates preprocess logic. Checks that arg list contains nonnull repo name object
 *
 * @author lanwen (Merkushev Kirill)
 * @see <a href=https://wiki.jenkins-ci.org/display/JENKINS/Web+Method>Web Method</a>
 */
@Retention(RUNTIME)
@Target({METHOD, FIELD})
@InterceptorAnnotation(ValidateRepoName.Processor.class)
public @interface ValidateRepoName {
    class Processor extends Interceptor {

        @Override
        public Object invoke(StaplerRequest request, StaplerResponse response, Object instance, Object[] arguments)
                throws IllegalAccessException, InvocationTargetException, ServletException {

            if (!from(newArrayList(arguments)).firstMatch(instanceOf(GitHubRepositoryName.class)).isPresent()) {
                throw new InvocationTargetException(
                        errorWithoutStack(SC_BAD_REQUEST, "Request should contain full repo name")
                );
            }

            return target.invoke(request, response, instance, arguments);
        }
    }
}


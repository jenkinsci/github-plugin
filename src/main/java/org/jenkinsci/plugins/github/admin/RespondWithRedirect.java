package org.jenkinsci.plugins.github.admin;

import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.Interceptor;
import org.kohsuke.stapler.interceptor.InterceptorAnnotation;

import javax.servlet.ServletException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * InterceptorAnnotation annotation to use on WebMethod signature.
 * Helps to redirect to prev page after web-method invoking.
 * WebMethod can return {@code void}
 *
 * @author lanwen (Merkushev Kirill)
 * @see <a href=https://wiki.jenkins-ci.org/display/JENKINS/Web+Method>Web Method</a>
 */
@Retention(RUNTIME)
@Target({METHOD, FIELD})
@InterceptorAnnotation(RespondWithRedirect.Processor.class)
public @interface RespondWithRedirect {
    class Processor extends Interceptor {

        @Override
        public Object invoke(StaplerRequest request, StaplerResponse response, Object instance, Object[] arguments)
                throws IllegalAccessException, InvocationTargetException, ServletException {
            target.invoke(request, response, instance, arguments);
            throw new InvocationTargetException(new HttpRedirect("."));
        }
    }
}


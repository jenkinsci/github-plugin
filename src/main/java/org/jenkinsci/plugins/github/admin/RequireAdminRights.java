package org.jenkinsci.plugins.github.admin;

import jenkins.model.Jenkins;
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
 * Encapsulates preprocess logic of checking for admin rights
 *
 * @author lanwen (Merkushev Kirill)
 * @see <a href=https://wiki.jenkins-ci.org/display/JENKINS/Web+Method>Web Method</a>
 */
@Retention(RUNTIME)
@Target({METHOD, FIELD})
@InterceptorAnnotation(RequireAdminRights.Processor.class)
public @interface RequireAdminRights {
    class Processor extends Interceptor {

        @Override
        public Object invoke(StaplerRequest request, StaplerResponse response, Object instance, Object[] arguments)
                throws IllegalAccessException, InvocationTargetException, ServletException {

            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
            return target.invoke(request, response, instance, arguments);
        }
    }
}


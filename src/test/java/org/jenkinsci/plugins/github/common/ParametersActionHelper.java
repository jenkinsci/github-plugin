package org.jenkinsci.plugins.github.common;

import hudson.model.ParametersAction;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Helper class to check if the environment includes SECURITY-170 fix
 *
 * @see <a href=https://wiki.jenkins-ci.org/display/JENKINS/Plugins+affected+by+fix+for+SECURITY-170</a>
 */
public class ParametersActionHelper {

    private static final Class<ParametersAction> actionClass = ParametersAction.class;

    private boolean hasSafeParameterConfig = false;
    private boolean abletoInspect = true;

    public ParametersActionHelper() {
        try {
            for (Field field : actionClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && isSafeParamsField(field)) {
                    this.hasSafeParameterConfig = true;
                    break;
                }
            }
        } catch (Exception e) {
            this.abletoInspect = false;
        }
    }

    /**
     * Method to check if the fix for SECURITY-170 is present
     *
     * @return true if the SECURITY-170 fix is present, false otherwise
     */
    public boolean getHasSafeParameterConfig() {
        return hasSafeParameterConfig;
    }

    /**
     * Method to check if this class has been able to determine the existence of SECURITY-170 fix
     *
     * @return true if the check for SECURITY-170 has been executed (whatever the result) false otherwise
     */
    public boolean getAbletoInspect() {
        return abletoInspect;
    }

    private boolean isSafeParamsField(Field field) {
        String fieldName = field.getName();
        return fieldName.equals("KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME")
                                                 || fieldName.equals("SAFE_PARAMETERS_SYSTEM_PROPERTY_NAME");
    }



}

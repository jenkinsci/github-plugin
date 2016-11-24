package org.jenkinsci.plugins.github.common;

import hudson.model.ParametersAction;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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

    public boolean getHasSafeParameterConfig() {
        return hasSafeParameterConfig;
    }

    public boolean getAbletoInspect() {
        return abletoInspect;
    }

    private boolean isSafeParamsField(Field field) {
        String fieldName = field.getName();
        return fieldName.equals("KEEP_UNDEFINED_PARAMETERS_SYSTEM_PROPERTY_NAME")
                                                 || fieldName.equals("SAFE_PARAMETERS_SYSTEM_PROPERTY_NAME");
    }



}

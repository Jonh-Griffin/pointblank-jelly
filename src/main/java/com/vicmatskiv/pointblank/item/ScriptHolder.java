package com.vicmatskiv.pointblank.item;

import com.vicmatskiv.pointblank.PointBlankJelly;
import groovy.lang.Script;

import javax.annotation.Nullable;

public interface ScriptHolder {
    @Nullable
    Script getScript();

    default boolean hasScript() {
        return getScript() != null;
    }

    default boolean hasFunction(String functionName) {
        return getScript() != null && getScript().getMetaClass().getMethods().stream().anyMatch(metaMethod -> metaMethod.getName().equalsIgnoreCase(functionName));
    }

    default Object invokeFunction(String functionName, Object... args) {
        if (getScript() == null)
            return null;
        if(!hasFunction(functionName)) {
            PointBlankJelly.LOGGER.debug("Function {} not found in script: {}", functionName, getScript().getClass().getName());
            return null;
        }
        try {
            return getScript().invokeMethod(functionName, args);
        } catch (Exception e) {
            PointBlankJelly.LOGGER.debug("Failed to invoke function {} in script: {}", functionName, getScript().getClass().getName(), e);
            return null;
        }
    }
}

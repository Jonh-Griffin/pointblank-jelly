package com.vicmatskiv.pointblank.feature;

import com.vicmatskiv.pointblank.Nameable;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;

import javax.naming.Name;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class FeatureScript<F extends Feature> implements Nameable {
    private final Script internalScript;
    private final String name;
    private Logger scriptLogger;

    public FeatureScript(String name, Script script) {
        this.internalScript = script;
        this.name = name;
        this.scriptLogger = Logger.getLogger(name);
    }

    @Override
    public String getName() {
        return name;
    }

    public void runFunction(String functionName, Object... args) {
        try {
            internalScript.invokeMethod(functionName, args);
        } catch (MissingMethodException e) {
            scriptLogger.warning("Method " + functionName + " not found in groovy script: " + name);
        }
    }
}

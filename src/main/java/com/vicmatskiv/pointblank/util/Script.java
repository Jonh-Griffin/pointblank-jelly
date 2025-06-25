package com.vicmatskiv.pointblank.util;

import dev.latvian.mods.rhino.Function;
import dev.latvian.mods.rhino.ScriptableObject;

import static com.vicmatskiv.pointblank.util.ScriptParser.shell;

public record Script(String id, dev.latvian.mods.rhino.Script script, ScriptableObject scope) {

    public Object invokeMethod(String methodName, Object... finalArgs) {
        Object obj = scope.get(shell, methodName, scope);
        if(obj instanceof Function func) {
            return func.call(shell, scope, scope, finalArgs);
        }
        return null;
    }
    public <T> T invokeMethod(String methodName, Class<T> returnType, Object... args) {
        Object obj = scope.get(shell, methodName, scope);
        if(obj instanceof Function func) {
            Object result = func.call(shell, scope, scope, args);
            if(returnType.isInstance(result)) {
                return returnType.cast(result);
            } else {
                throw new ClassCastException("Expected return type " + returnType.getName() + " but got " + result.getClass().getName());
            }
        }
        throw new IllegalStateException("No function found with name: " + methodName);
    }

    public boolean hasFunction(String functionName) {
        Object obj = scope.get(shell, functionName, scope);
        return obj instanceof Function;
    }

    public void run() {
        script.exec(shell, scope);
    }
}

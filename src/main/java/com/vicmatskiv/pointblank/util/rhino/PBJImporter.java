package com.vicmatskiv.pointblank.util.rhino;

import org.mozilla.javascript.*;

public class PBJImporter extends ImporterTopLevel {
    {
        try {
            put("jImport", this, new NativeJavaMethod(getClass().getMethod("jImport", Scriptable.class, Object.class, Object[].class), "jImport"));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    public static Object jImport(Scriptable scope, Object startPkg, Object... classes) {
        for (int i = 0; i != classes.length; i++) {
            Object arg = classes[i];
            if (!(arg instanceof NativeJavaClass)) {
                throw Context.reportRuntimeError(arg + " is not a Java Class!");
            }
            importClass(scope, (NativeJavaClass) arg);
        }
        return Undefined.instance;
    }

    private static void importClass(Scriptable scope, NativeJavaClass cl) {
        String s = cl.getClassObject().getName();
        String n = s.substring(s.lastIndexOf('.') + 1);
        Object val = scope.get(n, scope);
        if (val != NOT_FOUND) {
            if (val.equals(cl)) {
                return; // do not redefine same class
            }
            throw Context.reportRuntimeError("PBJImporter: class " + n + " already defined in scope");
        }
        // defineProperty(n, cl, DONTENUM);
        scope.put(n, scope, cl);
    }
}

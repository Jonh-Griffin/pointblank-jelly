package mod.pbj.script;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaClass;

import java.util.logging.Logger;

import static mod.pbj.script.ScriptParser.scope;
import static mod.pbj.script.PBJImporter.importClass;

public final class JLib {
    public static final Logger LOGGER = Logger.getLogger("PBJ-Scripts");

    public static void jImport(Object[] obj) {
        String startPkg = obj[0].toString();
        for(Object arg : obj) {
            if(arg == obj[0]) continue;
            if (!(arg instanceof String)) {
                throw Context.reportRuntimeError("PBJImporter: jImport requires class names as arguments");
            }
            try {
                Class<?> cl = Class.forName(startPkg + "." + arg);
                importClass(scope, new NativeJavaClass(scope, cl));
            } catch (ClassNotFoundException e) {
                throw Context.reportRuntimeError("PBJImporter: class " + arg + " not found");
            }
        }
    }

    public static void println(Object message) {
        System.out.println(message);
    }
    public static void print(Object message) {
        System.out.print(message);
    }
    public static void log(Object message) {
        LOGGER.info(message.toString());
    }
    public static void logError(Object message) {
        LOGGER.severe(message.toString());
    }
    public static void logWarning(Object message) {
        LOGGER.warning(message.toString());
    }
}

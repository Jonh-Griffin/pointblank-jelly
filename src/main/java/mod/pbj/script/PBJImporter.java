package mod.pbj.script;

import mod.pbj.Config;
import mod.pbj.PointBlankJelly;
import mod.pbj.feature.ConditionContext;
import mod.pbj.feature.Feature;
import mod.pbj.feature.Features;
import mod.pbj.item.ArmorItem;
import mod.pbj.item.GunItem;
import mod.pbj.util.ClientUtil;
import mod.pbj.util.Conditions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.mozilla.javascript.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static mod.pbj.script.ScriptParser.scope;
import static mod.pbj.script.ScriptParser.shell;

public class PBJImporter extends ImporterTopLevel {
    public static final Class<?>[] DEFAULT_IMPORTS = new Class[] {
      GunItem.class, ConditionContext.class, Conditions.class, ArmorItem.class, ItemStack.class, Item.class, Features.class,
            ClientUtil.class, Config.class, Feature.class, PointBlankJelly.class, System.class
    };
    {
        initStandardObjects(shell(), false);
        for(Class<?> cl : DEFAULT_IMPORTS) {
            importClass(this, new NativeJavaClass(this, cl));
        }
        for(Method method : JLib.class.getDeclaredMethods()) {
            addScopeFunction(this, method.getName(), method);
        }
    }
    public static void addScopeFunction(ScriptableObject obj, String name, Method method) {
        ScriptableObject.putProperty(
                obj,
                name,
                new BaseFunction() {
                    @Override
                    public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                        try {
                            if (method.getReturnType() == Void.class) {
                                method.invoke(null, args);
                                return Undefined.instance; // Or return a result
                            }
                            return method.invoke(null, new Object[] {args});
                        } catch (RuntimeException | IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException("Error invoking method: " + method.getName() + " with args " + Arrays.toString(args), e);
                        }
                    }
                }
        );
    }

    private static void jImport(Object[] args) {
        String startPkg = args[0] instanceof String ? (String) args[0] : "mod.pbj";
        for(Object arg : args) {
            if (!(arg instanceof String)) {
                throw Context.reportRuntimeError("PBJImporter: jImport requires class names as arguments");
            }
            try {
                Class<?> cl = Class.forName(((String) startPkg).concat("." + arg));
                importClass(scope, new NativeJavaClass(scope, cl));
            } catch (ClassNotFoundException e) {
                throw Context.reportRuntimeError("PBJImporter: class " + arg + " not found");
            }
        }
    }
    public static void importClass(Scriptable scope, NativeJavaClass cl) {
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

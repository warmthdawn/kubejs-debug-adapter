package com.warmthdawn.kubejsdebugadapter.utils;

import com.warmthdawn.kubejsdebugadapter.api.DebuggableObject;
import dev.latvian.mods.kubejs.recipe.RecipeFunction;
import dev.latvian.mods.rhino.*;
import dev.latvian.mods.rhino.util.DynamicFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VariableUtils {

    private static final Logger log = LogManager.getLogger();

    public static String variableToString(ContextFactory factory, Object value) {
        return factory.call(cx -> {
            if (value == Undefined.instance) {
                return "undefined";
            } else if (value == null) {
                return "null";
            } else if (value instanceof NativeCall) {
                return "[object Call]";
            } else if (value instanceof DynamicFunction) {
                return "[object DynamicFunction]";
            } else if (value instanceof NativeJavaMethod) {
                return "[object NativeJavaMethod]";
            } else if (value instanceof RecipeFunction) {
                return "[object RecipeFunction:" + value + "]";
            } else {
                try {
                    return Context.toString(value);
                } catch (RhinoException e) {
                    log.warn("Failed to convert variable to string, fallback to Object::toString()", e);
                    return value.toString();
                }
            }
        });
    }

    public static Object getObjectProperty(ContextFactory factory, Object object, Object id) {
        return factory.call((cx) -> {
            if (!(object instanceof Scriptable)) {
                return Undefined.instance;
            }
            Scriptable scriptable = (Scriptable) object;
            Object result;
            if (id instanceof String) {
                String name = (String) id;
                if (name.equals("this")) {
                    result = scriptable;
                } else if (name.equals("__proto__")) {
                    result = scriptable.getPrototype();
                } else if (name.equals("__parent__")) {
                    result = scriptable.getParentScope();
                } else {
                    result = ScriptableObject.getProperty(scriptable, name);
                    if (result == ScriptableObject.NOT_FOUND) {
                        result = Undefined.instance;
                    }
                }
            } else {
                int index = (Integer) id;
                result = ScriptableObject.getProperty(scriptable, index);
                if (result == ScriptableObject.NOT_FOUND) {
                    result = Undefined.instance;
                }
            }
            return result;
        });
    }

    public static Object[] getObjectIds(ContextFactory factory, Object obj) {
        return getObjectIds(factory, obj, false);
    }

    public static Object[] getObjectIds(ContextFactory factory, Object obj, boolean includeParent) {
        return factory.call((cx) -> {
            if (!(obj instanceof Scriptable) || obj == Undefined.instance) {
                return Context.emptyArgs;
            }

            Object[] ids;
            Scriptable scriptable = (Scriptable) obj;
            if (scriptable instanceof DebuggableObject) {
                ids = ((DebuggableObject) scriptable).getAllIds();
            } else {
                ids = scriptable.getIds();
            }

            Scriptable proto = scriptable.getPrototype();
            Scriptable parent = scriptable.getParentScope();
            int extra = 0;
            if (proto != null) {
                ++extra;
            }
            Object[] parents = new Object[0];
            if (parent != null) {
                if (includeParent) {
                    parents = getObjectIds(factory, parent, true);
                } else {
                    parents = new Object[]{"__parent__"};
                }
                extra += parents.length;
            }
            if (extra != 0) {
                Object[] tmp = new Object[extra + ids.length];
                System.arraycopy(ids, 0, tmp, extra, ids.length);
                ids = tmp;
                extra = 0;
                if (proto != null) {
                    ids[extra++] = "__proto__";
                }
                if (parent != null) {
                    if (includeParent) {
                        System.arraycopy(parents, 0, tmp, extra, parents.length);
                    } else {
                        ids[extra++] = "__parent__";
                    }
                }
            }

            return ids;


        });
    }

    public static boolean isPrimitive(Object variable) {
        if (variable == null) {
            return true;
        } else if (variable == Undefined.instance) {
            return true;
        } else if (variable instanceof CharSequence) {
            return true;
        } else if (variable instanceof Number) {
            return true;
        } else if (variable instanceof Boolean) {
            return true;
        } else if (variable instanceof Symbol) {
            return true;
        }
        return false;

    }

    public static String variableType(ContextFactory factory, Object variable) {
        return factory.call((cx) -> {
            if (variable == null) {
                return "null";
            } else if (variable == Undefined.instance) {
                return "undefined";
            } else if (variable instanceof CharSequence) {
                return "string";
            } else if (variable instanceof Number) {
                return "number";
            } else if (variable instanceof Boolean) {
                return "boolean";
            } else if (variable instanceof Symbol) {
                return "symbol";
            } else if (variable instanceof Scriptable) {
                try {
                    return ((Scriptable) variable).getClassName();
                } catch (Exception e) {
                    log.warn("Failed to get vairable class name for: " + variable, e);
                    if (variable instanceof Function) {
                        return "function";
                    }
                    return "object";
                }

            } else {

                return "object";
            }
        });
    }

}

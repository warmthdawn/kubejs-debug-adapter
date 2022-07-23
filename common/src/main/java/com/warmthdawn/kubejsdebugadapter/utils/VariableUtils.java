package com.warmthdawn.kubejsdebugadapter.utils;

import com.warmthdawn.kubejsdebugadapter.api.DebuggableScript;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.FunctionSourceData;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.ScriptSourceData;
import com.warmthdawn.kubejsdebugadapter.debugger.DebugRuntime;
import dev.latvian.mods.kubejs.recipe.RecipeFunction;
import dev.latvian.mods.rhino.*;
import dev.latvian.mods.rhino.regexp.NativeRegExp;
import dev.latvian.mods.rhino.util.DynamicFunction;
import joptsimple.internal.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

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
            } else if (value instanceof NativeJSON) {
                return "[object JSON]";
            } else {
                try {
                    return Context.toString(value);
                } catch (Throwable e) {
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
            if (!(obj instanceof Scriptable scriptable) || obj == Undefined.instance) {
                return Context.emptyArgs;
            }


            Object[] ids = scriptable.getAllIds();

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

    public static boolean isPrimitive(Object obj) {
        if (obj == null) {
            return true;
        }

        if (obj == Undefined.instance) {
            return true;
        }

        if (obj instanceof CharSequence) {
            return true;
        }

        if (obj instanceof Number) {
            return true;
        }

        if (obj instanceof Boolean) {
            return true;
        }

        if (obj instanceof NativeRegExp) {
            return true;
        }


        if (obj instanceof Scriptable) {
            String clazzName = ((Scriptable) obj).getClassName();

            if (Objects.equals("String", clazzName)) {
                return true;
            }

            if (Objects.equals("Number", clazzName)) {
                return true;
            }
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
            } else if (variable instanceof NativeRegExp || variable instanceof Pattern) {
                return "RegExp";
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

    public static boolean shouldShowChildren(ContextFactory factory, Object obj) {
        if (isPrimitive(obj)) {
            return false;
        }

        if (obj instanceof NativeJavaClass) {
            return true;
        }

        if (obj instanceof NativeJavaMethod) {
            return false;
        }

        if (obj instanceof RecipeFunction) {
            return false;
        }
        if (obj instanceof DynamicFunction) {
            return false;
        }

        return true;

    }

    public static String variableValue(ContextFactory factory, Object obj) {
        if (obj instanceof DebuggableScript && ((DebuggableScript) obj).isFunction()) {
            if (((DebuggableScript) obj).getSourceName() != null) {
                String sourceId = ((DebuggableScript) obj).getSourceName();
                String source = PathUtil.getSourcePath(sourceId).toString();
                ScriptSourceData scriptSourceData = DebugRuntime.getInstance().getSourceManager().getSourceData(sourceId);
                if (scriptSourceData != null) {
                    FunctionSourceData sourceData = scriptSourceData.getFunction(((DebuggableScript) obj).getFunctionScriptId());
                    LocationParser locationParser = scriptSourceData.getLocationParser();
                    return "@ " + source + ":" + locationParser.toLocation(sourceData.getPosition()).getLineNumber();
                }
            }
        }

        if (obj instanceof NativeJavaMethod method) {
            String functionName = method.getFunctionName();
            if (Strings.isNullOrEmpty(functionName)) {
                functionName = "f";
            }
            return functionName + " () { [NativeJavaMethod] }";
        }
        if (obj instanceof RecipeFunction recipeFunction) {
            return "f () { [RecipeFunction: " + recipeFunction.typeID + "] }";
        }
        if (obj instanceof DynamicFunction) {
            return "f () { [DynamicFunction] }";
        }

        if (obj instanceof NativeJavaArray || obj instanceof NativeJavaList) {

            Object unwrap = ((NativeJavaObject) obj).unwrap();
            int size = -1;
            IntFunction<Object> supplier = i -> Undefined.instance;
            if (unwrap instanceof List<?> list) {
                size = list.size();
                supplier = list::get;
            }
            if (unwrap instanceof Object[] array) {
                size = array.length;
                supplier = i -> array[i];
            }


            StringBuilder sb = new StringBuilder("[");

            for (int i = 0; i < size; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(supplier.apply(i));
            }

            return sb.append(']').toString();
        }


        return VariableUtils.variableToString(factory, obj);


    }

}

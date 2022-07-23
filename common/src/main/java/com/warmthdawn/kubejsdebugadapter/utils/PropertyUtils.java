package com.warmthdawn.kubejsdebugadapter.utils;

import com.warmthdawn.kubejsdebugadapter.data.variable.VariableDescriptor;
import dev.latvian.mods.kubejs.script.TypedDynamicFunction;
import dev.latvian.mods.rhino.*;
import dev.latvian.mods.rhino.regexp.NativeRegExp;
import dev.latvian.mods.rhino.util.DynamicFunction;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Objects;

public class PropertyUtils {


    private static Object getFieldValue(Context cx, Field field, Scriptable scope, Object obj) {
        if (field == null) {
            return null;
        }
        try {
            Object rval = field.get(obj);
            // Need to wrap the object before we return it.
            Scriptable topLevel = ScriptableObject.getTopLevelScope(scope);
            return cx.getWrapFactory().wrap(cx, topLevel, rval, field.getType());
        } catch (IllegalAccessException e) {
        }
        return null;

    }


    private static Field field_nativeJavaObject_members;

    private static Field field_javaMembers_members;
    private static Field field_javaMembers_staticMembers;


    private static Field field_fieldAndMethod_field;

    private static Field field_beanProperty_setter;
    private static Field field_beanProperty_setters;

    private static Method method_scriptableObject_getOwnPropertyDescriptor;

    static {
        try {
            Class<?> clazz_javaMembers = Kit.classOrNull("dev.latvian.mods.rhino.JavaMembers");

            field_nativeJavaObject_members = NativeJavaObject.class.getDeclaredField("members");

            field_javaMembers_members = clazz_javaMembers.getDeclaredField("members");
            field_javaMembers_staticMembers = clazz_javaMembers.getDeclaredField("staticMembers");

            field_fieldAndMethod_field = FieldAndMethods.class.getDeclaredField("field");

            field_beanProperty_setter = BeanProperty.class.getDeclaredField("setter");
            field_beanProperty_setters = BeanProperty.class.getDeclaredField("setters");

            method_scriptableObject_getOwnPropertyDescriptor = ScriptableObject.class.getDeclaredMethod("getOwnPropertyDescriptor", Context.class, Object.class);

            field_nativeJavaObject_members.setAccessible(true);
            field_javaMembers_members.setAccessible(true);
            field_javaMembers_staticMembers.setAccessible(true);
            field_fieldAndMethod_field.setAccessible(true);
            field_beanProperty_setter.setAccessible(true);
            field_beanProperty_setters.setAccessible(true);
            method_scriptableObject_getOwnPropertyDescriptor.setAccessible(true);


        } catch (NoSuchFieldException | NoSuchMethodException ignored) {
        }
    }

    @Nullable
    private static <T> T getField(Field field, Object obj) {
        if (field == null) {
            return null;
        }
        try {
            //noinspection unchecked
            return (T) field.get(obj);
        } catch (IllegalAccessException | NullPointerException e) {
            return null;
        }
    }

    @Nullable
    private static <T> T invokeMethod(Method method, Object obj, Object... prarms) {
        if (method == null) {
            return null;
        }
        try {
            //noinspection unchecked
            return (T) method.invoke(obj, prarms);
        } catch (IllegalAccessException | InvocationTargetException | NullPointerException e) {
            return null;
        }
    }


    public static VariableDescriptor defaultDescriptor(Context cx, Scriptable obj, Object key) {
        if (key instanceof Symbol symbolKey) {
            if (obj instanceof SymbolScriptable && ((SymbolScriptable) obj).has(symbolKey, obj)) {
                return VariableDescriptor.createNormal(key.toString());
            }

        } else if (key instanceof String stringKey) {
            if (obj.has(stringKey, obj)) {
                return VariableDescriptor.createNormal(stringKey);
            }

        } else if (key instanceof Number numberKey) {
            if (obj.has(numberKey.intValue(), obj)) {
                return VariableDescriptor.createNormal(String.valueOf(numberKey));
            }
        }
        return null;
    }

    public static VariableDescriptor getDescriptor(Context cx, NativeJavaObject obj, Object key) {

        if (key instanceof Number || key instanceof Symbol) {
            return defaultDescriptor(cx, obj, key);
        }

        String name = ScriptRuntime.toString(key);

        Object javaMember = getField(field_nativeJavaObject_members, obj);

        Map<String, Object> staticMembers = getField(field_javaMembers_staticMembers, javaMember);
        if (staticMembers == null)
            return null;

        Object member = null;
        if (!(obj instanceof NativeJavaClass)) {
            Map<String, Object> members = getField(field_javaMembers_members, javaMember);
            if (members == null)
                return null;
            member = members.get(name);
        }
        if (member == null) {
            member = staticMembers.get(name);
        }

        if (member == null) {
            return defaultDescriptor(cx, obj, key);
        } else if (member instanceof Scriptable) {
            if (member instanceof FieldAndMethods fam) {
                Field field = getField(field_fieldAndMethod_field, fam);
                boolean readonly = field != null && Modifier.isFinal(field.getModifiers());
                return VariableDescriptor.createField(name, readonly);
            } else if (member instanceof Function method) {
                return VariableDescriptor.createMethod(name);
            } else {
                return VariableDescriptor.createNormal(name);
            }
        } else if (member instanceof BeanProperty bp) {
            boolean readonly = getField(field_beanProperty_setter, bp) != null &&
                getField(field_beanProperty_setters, bp) != null;
            return VariableDescriptor.createLazy(name, readonly);
        } else if (member instanceof Field field) {
            // 字段
            boolean readonly = Modifier.isFinal(field.getModifiers());
            return VariableDescriptor.createField(name, readonly);
        }

        return null;
    }

    public static VariableDescriptor getDescriptor(Context cx, ScriptableObject obj, Object key) {
        Scriptable descriptor = invokeMethod(method_scriptableObject_getOwnPropertyDescriptor, obj, cx, key);
        if (descriptor == null) {
            return defaultDescriptor(cx, obj, key);
        }

        boolean readonly = descriptor.get("writable", descriptor) != Boolean.TRUE;
        boolean isLazy = descriptor.has("get", descriptor);
        return VariableDescriptor.createProperty(key.toString(), readonly, isLazy);
    }

    public static VariableDescriptor getDescriptor(ContextFactory factory, Scriptable obj, Object key) {
        return factory.call(cx -> {
            if (obj instanceof NativeJavaObject) {
                return getDescriptor(cx, (NativeJavaObject) obj, key);
            } else if (obj instanceof ScriptableObject) {
                return getDescriptor(cx, (ScriptableObject) obj, key);
            } else {
                return defaultDescriptor(cx, obj, key);
            }
        });
    }




}

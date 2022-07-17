package com.warmthdawn.kubejsdebugadapter.utils;

import dev.latvian.mods.rhino.*;
import dev.latvian.mods.rhino.regexp.NativeRegExp;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Objects;

public class VariableUtilPlus {


    private static Object getFieldValue(ContextFactory factory, Field field, Scriptable scope, Object obj) {
        return factory.call((cx) -> {
            try {
                Object rval = field.get(obj);
                // Need to wrap the object before we return it.
                Scriptable topLevel = ScriptableObject.getTopLevelScope(scope);
                return cx.getWrapFactory().wrap(cx, topLevel, rval, field.getType());
            } catch (IllegalAccessException e) {
                // TODO: 返回一个出错的结果类型
            }
            return null;
        });
    }

    private static void resolveScriptableObject(ContextFactory factory, ScriptableObject obj) {

    }

    @SuppressWarnings("unchecked")
    private static void resolveNormalNativeJavaObjectChildren(ContextFactory factory, NativeJavaObject obj) {
        Object membersObj = ReflectionUtils.getField(obj, "members");

        Object javaObject = ReflectionUtils.getField(obj, "javaObject");
        Map<String, Object> members =
            (Map<String, Object>) ReflectionUtils.getField(membersObj, "members");

        if (members == null) {
            return;
        }


        for (Map.Entry<String, Object> entry : members.entrySet()) {

            String name = entry.getKey();

            Object member = entry.getValue();

            String type = null;
            boolean shouldLazyGet = false;
            Object value = null;

            boolean readonly = false;

            if (member instanceof Scriptable) {
                // TODO: 解析 Scriptable的类型（js内部类型）

                if (member instanceof FieldAndMethods fam) {
                    type = "field";
                    Field field = (Field) ReflectionUtils.getField(fam, "field");
                    readonly = field != null && Modifier.isFinal(field.getModifiers());
                    value = getFieldValue(factory, field, obj, javaObject);

                }

                if (member instanceof NativeJavaMethod) {
                    type = "method";
                    readonly = true;
                    value = member;
                }

            } else if (member instanceof BeanProperty bp) {
                // 属性
                type = "property";
                shouldLazyGet = true;

                if (ReflectionUtils.getField(bp, "setter") != null &&
                    ReflectionUtils.getField(bp, "setters") != null) {
                    readonly = true;
                }


            } else if (member instanceof Field field) {
                // 字段
                type = "field";
                readonly = Modifier.isFinal(field.getModifiers());
                value = getFieldValue(factory, field, obj, javaObject);

            } else  {
                // TODO: 报错表示无法解析
            }

        }

    }


    public static void resolveVariable(ContextFactory factory, Object obj) {

        if (isPrimaryType(obj)) {
            return;
        }

        // 如果这个变量是一个java的内部类型
        if (obj instanceof NativeJavaObject) {
            if (obj instanceof NativeJavaClass) {

            } else if (obj instanceof NativeJavaArray || obj instanceof NativeJavaList) {
                // 数组
            } else if (obj instanceof NativeJavaMap) {
                // map
            } else {
                // 普通java对象
                resolveNormalNativeJavaObjectChildren(factory, (NativeJavaObject) obj);
            }


        }

        // Javascript的内部类型
        if (obj instanceof IdScriptableObject) {
        }


    }

    private static boolean isPrimaryType(Object obj) {

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

}

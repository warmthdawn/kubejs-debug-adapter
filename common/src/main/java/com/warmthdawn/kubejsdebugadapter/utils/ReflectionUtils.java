package com.warmthdawn.kubejsdebugadapter.utils;

import java.lang.reflect.Field;

public class ReflectionUtils {

    public static Object getField(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
        }
        return null;
    }
}

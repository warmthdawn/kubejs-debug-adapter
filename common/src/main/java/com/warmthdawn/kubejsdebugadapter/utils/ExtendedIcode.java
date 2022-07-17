package com.warmthdawn.kubejsdebugadapter.utils;

import java.lang.reflect.Field;

public class ExtendedIcode {


    private static int addIcode() {
        try {
            Class<?> clazz = Class.forName("dev.latvian.mods.rhino.Icode");
            Field min_icode = clazz.getDeclaredField("MIN_ICODE");
            int min = (int) min_icode.get(null);
            min_icode.set(null, min - 1);
            return min - 1;
        } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    public static final int Icode_BREAKPOINT = addIcode();
}

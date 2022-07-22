package com.warmthdawn.kubejsdebugadapter.utils;

import com.warmthdawn.kubejsdebugadapter.api.DebuggableScript;
import dev.latvian.mods.rhino.ObjArray;

import java.util.function.BooleanSupplier;

public class Utils {

    public static void waitFor(BooleanSupplier supplier) {
        while (!supplier.getAsBoolean()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

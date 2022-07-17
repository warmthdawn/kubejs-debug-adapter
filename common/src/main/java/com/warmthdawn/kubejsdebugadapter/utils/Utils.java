package com.warmthdawn.kubejsdebugadapter.utils;

import com.warmthdawn.kubejsdebugadapter.api.DebuggableScript;
import dev.latvian.mods.rhino.ObjArray;

public class Utils {


    /** Returns an array of all functions in the given script. */
    private static DebuggableScript[] getAllFunctions(DebuggableScript function) {
        ObjArray functions = new ObjArray();
        collectFunctions_r(function, functions);
        DebuggableScript[] result = new DebuggableScript[functions.size()];
        functions.toArray(result);
        return result;
    }

    /** Helper function for {@link #getAllFunctions(DebuggableScript)}. */
    private static void collectFunctions_r(DebuggableScript function, ObjArray array) {
        array.add(function);
        for (int i = 0; i != function.getFunctionCount(); ++i) {
            collectFunctions_r(function.getFunction(i), array);
        }
    }

}

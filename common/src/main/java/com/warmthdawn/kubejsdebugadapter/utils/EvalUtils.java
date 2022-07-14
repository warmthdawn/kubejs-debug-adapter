package com.warmthdawn.kubejsdebugadapter.utils;

import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.Scriptable;

public class EvalUtils {
    public static Object evalulate(ContextFactory factory, String expression, Scriptable scope) {
        return factory.call((cx) -> cx.evaluateString(scope, expression, "<eval>", 1, null));
    }
}

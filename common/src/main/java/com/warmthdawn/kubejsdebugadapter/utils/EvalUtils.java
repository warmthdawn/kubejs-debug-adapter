package com.warmthdawn.kubejsdebugadapter.utils;

import com.warmthdawn.kubejsdebugadapter.debugger.DebugContextData;
import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.Scriptable;

public class EvalUtils {
    public static Object evaluate(ContextFactory factory, String expression, Scriptable scope) {
        return factory.call((cx) -> {
            DebugContextData contextData = DebugContextData.get(cx);

            try {
                contextData.setEvaluating(true);
                return cx.evaluateString(scope, expression, "<eval>", 1, null);
            } finally {
                contextData.setEvaluating(false);
            }
        });
    }
}

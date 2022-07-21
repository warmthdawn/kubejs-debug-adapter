package com.warmthdawn.kubejsdebugadapter.debugger;

import com.warmthdawn.kubejsdebugadapter.adapter.DebuggerBridge;
import com.warmthdawn.kubejsdebugadapter.api.DebugFrame;
import com.warmthdawn.kubejsdebugadapter.api.DebuggableScript;
import com.warmthdawn.kubejsdebugadapter.api.Debugger;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.ScriptSourceData;
import dev.latvian.mods.rhino.Context;


public class DebuggerProxy implements Debugger {
    private final DebugRuntime runtime;
    private final String managerName;


    public DebuggerProxy(DebugRuntime runtime, String managerName) {
        this.runtime = runtime;
        this.managerName = managerName;
    }


    @Override
    public void handleCompilationDone(Context cx, DebuggableScript fnOrScript, String sourceString) {
        DebugContextData contextData = DebugContextData.get(cx);
        if (contextData.isEvaluating()) {
            return;
        }
        if (!fnOrScript.isTopLevel()) {
            return;
        }

        String sourceName = fnOrScript.getSourceName();


        DebuggerBridge bridge = runtime.getBridge();
        if (sourceName != null && bridge != null) {
            bridge.notifySource(sourceName);
        }


    }

    @Override
    public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript) {

        String sourceName = fnOrScript.getSourceName();
        ScriptSourceData scriptSourceData = runtime.getSourceManager().getSourceData(sourceName);
        if (scriptSourceData != null) {
            return new KubeStackFrame(
                runtime.nextFrameId(),
                runtime,
                fnOrScript,
                cx.getFactory(),
                scriptSourceData);
        }
        return null;
    }
}

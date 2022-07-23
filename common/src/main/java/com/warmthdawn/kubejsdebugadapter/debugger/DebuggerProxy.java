package com.warmthdawn.kubejsdebugadapter.debugger;

import com.warmthdawn.kubejsdebugadapter.adapter.DebuggerBridge;
import com.warmthdawn.kubejsdebugadapter.api.DebugFrame;
import com.warmthdawn.kubejsdebugadapter.api.DebuggableScript;
import com.warmthdawn.kubejsdebugadapter.api.Debugger;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.FunctionSourceData;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.ScriptSourceData;
import com.warmthdawn.kubejsdebugadapter.utils.LocationParser;
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
            bridge.notifySourceCompiled(sourceName);
        }


    }

    @Override
    public DebugFrame getFrame(Context cx, DebuggableScript fnOrScript) {
        String sourceName = fnOrScript.getSourceName();
        if (sourceName == null) {
            return null;
        }

        ScriptSourceData scriptSourceData = runtime.getSourceManager().getSourceData(sourceName);
        if (scriptSourceData == null) {
            return null;
        }
        int functionScriptId = fnOrScript.getFunctionScriptId();
        if (functionScriptId < 0) {
            return null;
        }
        FunctionSourceData sourceData = scriptSourceData.getFunction(functionScriptId);
        LocationParser locationParser = scriptSourceData.getLocationParser();
        return new KubeStackFrame(
            runtime.nextFrameId(),
            runtime,
            fnOrScript,
            cx.getFactory(),
            sourceData,
            locationParser

        );
    }
}

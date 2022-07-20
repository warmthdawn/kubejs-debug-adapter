package com.warmthdawn.kubejsdebugadapter.api;

import com.warmthdawn.kubejsdebugadapter.data.ScriptBreakpointData;
import com.warmthdawn.kubejsdebugadapter.data.ScriptDebuggerData;

public interface IDebuggableScriptProvider {
    ScriptDebuggerData getDebuggerData();

    void setDebuggerData(ScriptDebuggerData breakpointData);

    DebuggableScript getDebuggableScript();
}

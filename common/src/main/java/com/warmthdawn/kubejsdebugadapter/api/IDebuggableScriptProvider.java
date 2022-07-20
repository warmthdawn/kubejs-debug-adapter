package com.warmthdawn.kubejsdebugadapter.api;

import com.warmthdawn.kubejsdebugadapter.data.ScriptBreakpointData;
import com.warmthdawn.kubejsdebugadapter.data.ScriptDebuggerData;

public interface IDebuggableScriptProvider {

    void setFunctionScriptId(int id);

    DebuggableScript getDebuggableScript();
}

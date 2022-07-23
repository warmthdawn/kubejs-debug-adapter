package com.warmthdawn.kubejsdebugadapter.api;

public interface IDebuggableScriptProvider {

    void setFunctionScriptId(int id);

    DebuggableScript getDebuggableScript();
}

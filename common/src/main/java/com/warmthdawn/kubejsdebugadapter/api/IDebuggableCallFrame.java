package com.warmthdawn.kubejsdebugadapter.api;

import dev.latvian.mods.rhino.Scriptable;

public interface IDebuggableCallFrame {
    void setDebuggerFrame(DebugFrame frame);

    DebugFrame getDebuggerFrame();

    Scriptable getScope();

    Scriptable getThisObj();

    IDebuggableCallFrame getParent();

    Object getResult();

    double getResultDbl();


    void increasePC(int inc);

    int readBreakpointMeta(byte[] iCode);

}

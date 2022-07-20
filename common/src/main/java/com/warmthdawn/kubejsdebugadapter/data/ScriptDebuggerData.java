package com.warmthdawn.kubejsdebugadapter.data;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.List;

public class ScriptDebuggerData {
    private final int functionStart;
    private final int functionEnd;

    private final List<ScriptBreakpointData> possibleBreakpoints;

    public ScriptDebuggerData(int functionStart, int functionEnd) {
        this.functionStart = functionStart;
        this.functionEnd = functionEnd;
        this.possibleBreakpoints = new ArrayList();
    }

    public int getFunctionStart() {
        return functionStart;
    }

    public int getFunctionEnd() {
        return functionEnd;
    }

    public List<ScriptBreakpointData> getPossibleBreakpoints() {
        return possibleBreakpoints;
    }
}

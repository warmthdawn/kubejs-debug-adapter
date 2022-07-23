package com.warmthdawn.kubejsdebugadapter.data.breakpoint;

public class ScriptLocation {
    private final int lineNumber;
    private final int columnNumber;
    private final String sourceName;

    public ScriptLocation(int lineNumber, int columnNumber, String sourceName) {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.sourceName = sourceName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public String getSourceName() {
        return sourceName;
    }
}

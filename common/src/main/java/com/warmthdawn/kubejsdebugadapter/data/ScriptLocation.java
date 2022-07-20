package com.warmthdawn.kubejsdebugadapter.data;

public class ScriptLocation {
    private int lineNumber;
    private int columnNumber;
    private String sourceName;

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

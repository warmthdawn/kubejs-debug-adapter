package com.warmthdawn.kubejsdebugadapter.data.breakpoint;

public class UserDefinedBreakpoint {

    private int line;
    private int column;
    private int id;

    private String condition;
    private String logMessage;

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public void setLine(int lineno) {
        this.line = lineno;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getLogMessage() {
        return logMessage;
    }

    public void setLogMessage(String logMessage) {
        this.logMessage = logMessage;
    }
}

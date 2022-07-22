package com.warmthdawn.kubejsdebugadapter.data;

public class UserDefinedBreakpoint {

    private int line;
    private int column;
    private int id;

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
}

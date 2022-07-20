package com.warmthdawn.kubejsdebugadapter.data.breakpoint;

public class BreakpointMeta {
    private final int id;
    private final int position;
    private final int length;

    public BreakpointMeta(int id, int position, int length) {
        this.id = id;
        this.position = position;
        this.length = length;
    }

    public int getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public int getLength() {
        return length;
    }


}

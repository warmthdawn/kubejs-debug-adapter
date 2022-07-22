package com.warmthdawn.kubejsdebugadapter.data.breakpoint;

public class BreakpointMeta {
    private final int id;
    private final int position;
    private final int length;

    private boolean lowPriority;


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

    public void setLowPriority(boolean lowPriority) {
        this.lowPriority = lowPriority;
    }

    public boolean isLowPriority() {
        return lowPriority;
    }
}

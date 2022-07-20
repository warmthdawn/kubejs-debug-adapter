package com.warmthdawn.kubejsdebugadapter.data.breakpoint;

public class StatementBreakpointMeta extends BreakpointMeta {
    private boolean shouldBreak;

    public StatementBreakpointMeta(int id, int position, int length, boolean mustBreak) {
        super(id, position, length);
        if (mustBreak) {
            this.shouldBreak = true;
        }
    }

    /**
     * 是否需要在这里断点，如果它有子节点，那么它的子节点也会被断点，这时候这个表达式就不断了
     */
    public boolean shouldBreakHere() {
        return shouldBreak;
    }

    public void addChild(BreakpointMeta meta) {
        if (meta.getPosition() == getPosition()) {
            this.shouldBreak = true;
        }
    }
}

package com.warmthdawn.kubejsdebugadapter.data.breakpoint;

public class StatementBreakpointMeta extends BreakpointMeta {
    private boolean hasSameChild = false;
    private boolean mustBreak = false;

    public StatementBreakpointMeta(int id, int position, int length, boolean mustBreak) {
        super(id, position, length);
        this.mustBreak = mustBreak;
    }

    /**
     * 是否需要在这里断点，如果它有子节点，那么它的子节点也会被断点，这时候这个表达式就不断了
     */
    public boolean shouldBreakHere() {
        return mustBreak || !hasSameChild;
    }

    public void addChild(BreakpointMeta meta) {
        if (meta.getPosition() == getPosition()) {
            this.hasSameChild = true;
        }
    }
}

package com.warmthdawn.kubejsdebugadapter.debugger;

import com.warmthdawn.kubejsdebugadapter.api.IDebuggableContext;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ObjArray;

public class DebugContextData {
    /**
     * The stack frames.
     */

    private boolean isEvaluating = false;


    private final ObjArray frameStack = new ObjArray();

    /**
     * Whether the debugger should break at the next line in this context.
     */
    private boolean breakNextStmt;

    /**
     * The frame depth the debugger should stop at. Used to implement "step over" and "step
     * out".
     */
    private int stopAtFrameDepth = -1;

    private boolean shouldPause = false;

    /**
     * Whether this context is in the event thread.
     */
    private boolean eventThreadFlag;

    /**
     * The last exception that was processed.
     */
    private Throwable lastProcessedException;

    public DebugContextData() {
    }

    public boolean shouldPauseStep() {
        if (!breakNextStmt) {
            return false;
        }

        if (stopAtFrameDepth >= 0 && frameCount() > stopAtFrameDepth) {
            return false;
        }

        return true;
    }

    public void clearStepInfo() {
        stopAtFrameDepth = -1;
        breakNextStmt = false;
        shouldPause = false;
    }

    public boolean shouldPause() {
        return shouldPause;
    }

    public void pause() {
        shouldPause = true;
    }

    public void stepInto() {
        if (breakNextStmt) {
            return;
        }
        stopAtFrameDepth = -1;
        breakNextStmt = true;
    }

    public void stepOver() {
        if (breakNextStmt) {
            return;
        }
        stopAtFrameDepth = frameCount();
        breakNextStmt = true;
    }

    public void stepOut() {
        if (breakNextStmt) {
            return;
        }
        if (frameCount() > 0) {
            stopAtFrameDepth = frameCount() - 1;
            breakNextStmt = true;
        }
    }

    /**
     * Returns the ContextData for the given Context.
     */
    public static DebugContextData get(Context cx) {
        if (cx instanceof IDebuggableContext dcx) {
            return (DebugContextData) dcx.getDebuggerContextData();
        }
        return null;
    }

    /**
     * Returns the number of stack frames.
     */
    public int frameCount() {
        return frameStack.size();
    }

    /**
     * Returns the stack frame with the given index.
     */
    public KubeStackFrame getFrame(int frameNumber) {
        int num = frameStack.size() - frameNumber - 1;
        return (KubeStackFrame) frameStack.get(num);
    }

    /**
     * Pushes a stack frame on to the stack.
     */
    public void pushFrame(KubeStackFrame frame) {
        frameStack.push(frame);

    }

    /**
     * Pops a stack frame from the stack.
     */
    public void popFrame() {
        frameStack.pop();
    }

    public boolean isEvaluating() {
        return isEvaluating;
    }

    public void setEvaluating(boolean evaluating) {
        isEvaluating = evaluating;
    }

    public KubeStackFrame[] stackFrames() {
        KubeStackFrame[] result = new KubeStackFrame[frameStack.size()];
        frameStack.toArray(result);
        return result;
    }

}

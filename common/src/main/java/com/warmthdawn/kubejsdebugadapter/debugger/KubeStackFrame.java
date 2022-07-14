package com.warmthdawn.kubejsdebugadapter.debugger;

import com.warmthdawn.kubejsdebugadapter.adapter.DebuggerBridge;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.debug.DebugFrame;
import dev.latvian.mods.rhino.debug.DebuggableScript;
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;

public class KubeStackFrame implements DebugFrame {


    private final String[] paramNames;

    public KubeStackFrame(int id, DebugRuntime runtime, DebuggableScript function, ContextFactory factory) {
        this.id = id;
        this.runtime = runtime;
        this.source = function.getSourceName();
        this.functionName = function.getFunctionName();
        this.factory = factory;

        int paramCount = function.getParamCount();
        int paramAndVarCount = function.getParamAndVarCount();
        String[] paramNames = new String[paramCount];
        for (int i = 0; i < paramAndVarCount; i++) {
            paramNames[i] = function.getParamOrVarName(i);
        }

        this.paramNames = paramNames;
    }


    private final int id;
    private final ContextFactory factory;
    private final String functionName;
    private final String source;
    private final DebugRuntime runtime;
    private int currentLineNumber = 0;

    private Scriptable thisObj;
    private Object[] args;
    private Scriptable scope;


    private void handlePause(Context cx, String reason) {
        handlePause(cx, reason, true);
    }

    private void handlePause(Context cx, String reason, boolean interrupt) {
        DebugContextData data = DebugContextData.get(cx);
        if (data.isEvaluating()) {
            return;
        }
        DebuggerBridge bridge = runtime.getBridge();
        if (bridge == null) {
            data.clearStepInfo();
            return;
        }

        if (interrupt) {
            DebugThread thread = runtime.getThread(cx);
            bridge.notifyStop(thread.id(), reason);
            //暂停脚本执行
            thread.interrupt();
        }
        data.clearStepInfo();
    }

    @Override
    public void onEnter(Context cx, Scriptable activation, Scriptable thisObj, Object[] args) {
        DebugContextData data = DebugContextData.get(cx);
        data.pushFrame(this);

        this.thisObj = thisObj;
        this.args = args;
        this.scope = activation;

        DebuggerBridge bridge = runtime.getBridge();
        if (bridge == null) {
            data.clearStepInfo();
            return;
        }
        // 是否有函数断点
        if (bridge.hasFunctionBreakpointFor(functionName)) {
            DebugThread thread = runtime.getThread(cx);
            bridge.notifyStop(thread.id(), StoppedEventArgumentsReason.FUNCTION_BREAKPOINT);
            data.clearStepInfo();
        }
    }

    @Override
    public void onLineChange(Context cx, int lineNumber) {
        this.currentLineNumber = lineNumber;
        DebuggerBridge bridge = runtime.getBridge();
        if (bridge == null) {
            return;
        }
        DebugContextData data = DebugContextData.get(cx);
        // 暂停
        if (bridge.shouldPause() || data.shouldPause()) {
            handlePause(cx, StoppedEventArgumentsReason.PAUSE);
            return;
        }
        // 是否有断点
        if (bridge.hasBreakpointAt(this.source, lineNumber)) {
            handlePause(cx, StoppedEventArgumentsReason.BREAKPOINT);
            return;
        }
        // 是否正在进行单步调试
        if (data.shouldPauseStep()) {
            handlePause(cx, StoppedEventArgumentsReason.STEP);
        }
    }

    @Override
    public void onExceptionThrown(Context cx, Throwable ex) {
        DebuggerBridge bridge = runtime.getBridge();
        if (bridge == null) {
            return;
        }
        // 是否有异常断点
        if (bridge.shouldPauseOnException(ex)) {
            handlePause(cx, StoppedEventArgumentsReason.EXCEPTION);
        }
    }

    @Override
    public void onExit(Context cx, boolean byThrow, Object resultOrException) {
        // TODO: 如果断点打在方法最后一行？
        DebugContextData.get(cx).popFrame();
    }

    @Override
    public void onDebuggerStatement(Context cx) {
        DebuggerBridge bridge = runtime.getBridge();
        if (bridge == null) {
            return;
        }
        // js debugger;语句，直接断！
        handlePause(cx, StoppedEventArgumentsReason.BREAKPOINT);
    }

    public int currentLine() {
        return currentLineNumber;
    }

    public int getId() {
        return this.id;
    }

    public String getSource() {
        return source;
    }

    public ContextFactory getFactory() {
        return factory;
    }

    public Scriptable getThisObj() {
        return thisObj;
    }

    public Object[] getArgs() {
        return args;
    }

    public String[] getParamNames() {
        return paramNames;
    }

    public Scriptable getScope() {
        return scope;
    }
}

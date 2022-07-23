package com.warmthdawn.kubejsdebugadapter.debugger;

import com.warmthdawn.kubejsdebugadapter.adapter.DebuggerBridge;
import com.warmthdawn.kubejsdebugadapter.api.DebugFrame;
import com.warmthdawn.kubejsdebugadapter.api.DebuggableScript;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.ScriptLocation;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.UserDefinedBreakpoint;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.BreakpointMeta;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.FunctionSourceData;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.StatementBreakpointMeta;
import com.warmthdawn.kubejsdebugadapter.utils.LocationParser;
import dev.latvian.mods.rhino.*;
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;

public class KubeStackFrame implements DebugFrame {


    private final String[] paramNames;

    public KubeStackFrame(int id, DebugRuntime runtime, DebuggableScript function, ContextFactory factory, FunctionSourceData sourceData, LocationParser locationParser) {
        this.id = id;
        this.runtime = runtime;
        this.source = function.getSourceName();
        this.functionName = function.getFunctionName();
        this.function = function;
        this.factory = factory;
        this.sourceData = sourceData;
        this.locationParser = locationParser;
        int paramCount = function.getParamCount();
        String[] paramNames = new String[paramCount];
        for (int i = 0; i < paramCount; i++) {
            paramNames[i] = function.getParamOrVarName(i);
        }

        this.paramNames = paramNames;
    }


    private final int id;
    private FunctionSourceData sourceData;
    private LocationParser locationParser;
    private final ContextFactory factory;
    private final String functionName;
    private final String source;
    private final DebugRuntime runtime;
    private final DebuggableScript function;

    private Scriptable thisObj;
    private Object[] args;
    private Scriptable scope;

    private boolean interrupted = false;
    private ScriptLocation location = null;
    private ScriptLocation endLocation = null;


    public boolean isInterrupted() {
        return interrupted;
    }

    public int getLine() {
        if(location == null) {
            return -1;
        }
        return location.getLineNumber();
    }

    public int getColumn() {
        if(location == null) {
            return -1;
        }
        return location.getColumnNumber();
    }

    public int getEndLine() {
        if(endLocation == null) {
            return -1;
        }
        return endLocation.getLineNumber();
    }

    public int getEndColumn() {
        if(endLocation == null) {
            return -1;
        }
        return endLocation.getColumnNumber();
    }

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
            this.interrupted = true;
            //暂停脚本执行
            thread.interrupt();
            this.interrupted = false;
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

    private void updatePosition(int position, int length) {
        location = locationParser.toLocation(position);
        endLocation = locationParser.toLocation(position + length);
    }

    private boolean handleCommonBreakable(DebugContextData data, DebuggerBridge bridge, Context cx, BreakpointMeta meta) {
        if (data.isEvaluating()) {
            return true;
        }
        // 暂停
        if (bridge.shouldPause() || data.shouldPause()) {
            handlePause(cx, StoppedEventArgumentsReason.PAUSE);
            return true;
        }
        if (meta == null) {
            return false;
        }

        UserDefinedBreakpoint userDefinedBreakpoint = bridge.getBreakpointAt(this.source, locationParser, meta);
        // 是否有断点
        if (userDefinedBreakpoint != null) {
            // TODO: 其他的断点类型
            handlePause(cx, StoppedEventArgumentsReason.BREAKPOINT);
            return true;
        }
        return false;
    }

    @Override
    public void onBreakableStatement(Context cx, int meta) {
        DebuggerBridge bridge = runtime.getBridge();
        if (bridge == null) {
            return;
        }
        DebugContextData data = DebugContextData.get(cx);
        StatementBreakpointMeta breakpointMeta = sourceData.getStatementBreakpointMeta(meta);
        updatePosition(breakpointMeta.getPosition(), breakpointMeta.getLength());
        if (!breakpointMeta.shouldBreakHere()) {
            breakpointMeta = null;
        }
        if (handleCommonBreakable(data, bridge, cx, breakpointMeta)) {
            return;
        }

        // 是否正在进行单步调试
        if (data.shouldPauseStep()) {
            handlePause(cx, StoppedEventArgumentsReason.STEP);
        }
    }

    @Override
    public void onBreakableExpression(Context cx, int meta) {
        DebuggerBridge bridge = runtime.getBridge();
        if (bridge == null) {
            return;
        }
        DebugContextData data = DebugContextData.get(cx);

        BreakpointMeta breakpointMeta = sourceData.getExpressionBreakpointMeta(meta);
        updatePosition(breakpointMeta.getPosition(), breakpointMeta.getLength());
        handleCommonBreakable(data, bridge, cx, breakpointMeta);

    }

    @Override
    public void onExceptionThrown(Context cx, Throwable ex) {
        DebuggerBridge bridge = runtime.getBridge();
        if (bridge == null) {
            return;
        }
        // TODO: 异常位置
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

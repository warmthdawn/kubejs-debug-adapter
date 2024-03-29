package com.warmthdawn.kubejsdebugadapter.debugger;

import com.google.common.collect.ImmutableSet;
import com.warmthdawn.kubejsdebugadapter.adapter.DebuggerBridge;
import com.warmthdawn.kubejsdebugadapter.api.DebugFrame;
import com.warmthdawn.kubejsdebugadapter.api.DebuggableScript;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.ScriptLocation;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.UserDefinedBreakpoint;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.BreakpointMeta;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.FunctionSourceData;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.StatementBreakpointMeta;
import com.warmthdawn.kubejsdebugadapter.utils.EvalUtils;
import com.warmthdawn.kubejsdebugadapter.utils.LocationParser;
import dev.latvian.mods.rhino.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.debug.StoppedEventArgumentsReason;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class KubeStackFrame implements DebugFrame {


    private static final Logger log = LogManager.getLogger();
    private final String[] paramNames;
    private final Set<String> localNames;


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

        int paramAndVarCount = function.getParamAndVarCount();
        if (paramAndVarCount > paramCount) {
            String[] localNames = new String[paramAndVarCount - paramCount];
            for (int i = paramCount; i < paramAndVarCount; i++) {
                localNames[i - paramCount] = function.getParamOrVarName(i);
            }
            this.localNames = ImmutableSet.copyOf(localNames);
        } else {
            this.localNames = Collections.emptySet();
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
        if (location == null) {
            return -1;
        }
        return location.getLineNumber();
    }

    public int getColumn() {
        if (location == null) {
            return -1;
        }
        return location.getColumnNumber();
    }

    public int getEndLine() {
        if (endLocation == null) {
            return -1;
        }
        return endLocation.getLineNumber();
    }

    public int getEndColumn() {
        if (endLocation == null) {
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

        data.clearStepInfo();
        if (interrupt) {
            DebugThread thread = runtime.getThread(cx);
            bridge.notifyStop(thread.id(), reason);
            this.interrupted = true;
            log.info("The script is interrupted by a breakpoint.");
            //暂停脚本执行
            thread.interrupt();
            log.info("The script execution is resumed.");
            this.interrupted = false;
        }
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


        if (this.sourceData.isHasBlock()) {
            updatePosition(this.sourceData.getRcEnd(), 1);
            UserDefinedBreakpoint userDefinedBreakpoint = bridge.getBreakpointAt(this.source, locationParser, this.sourceData.getLcStart());
            if (handleBreakpoint(bridge, cx, userDefinedBreakpoint)) {
                return;
            }
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

    private boolean handleBreakpoint(DebuggerBridge bridge, Context cx, UserDefinedBreakpoint userDefinedBreakpoint) {
        if (userDefinedBreakpoint != null) {
            if (userDefinedBreakpoint.getCondition() != null) {
                try {
                    Object evaluate = EvalUtils.evaluate(factory, userDefinedBreakpoint.getCondition(), scope);
                    boolean match = ScriptRuntime.toBoolean(evaluate);
                    if (!match) {
                        return false;
                    }
                } catch (Throwable ignored) {
                    return false;
                }
            }

            if (userDefinedBreakpoint.getLogMessage() != null) {
                bridge.logMessage(factory, userDefinedBreakpoint.getLogMessage(), getScope());
            } else {
                handlePause(cx, StoppedEventArgumentsReason.BREAKPOINT);
            }
            return true;
        }
        return false;
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

        UserDefinedBreakpoint userDefinedBreakpoint = bridge.getBreakpointAt(this.source, locationParser, meta.getPosition());
        // 是否有断点
        return handleBreakpoint(bridge, cx, userDefinedBreakpoint);

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
        if (this.sourceData.isHasBlock()) {
            DebuggerBridge bridge = runtime.getBridge();
            if (bridge == null) {
                return;
            }
            updatePosition(this.sourceData.getRcEnd(), 1);
            UserDefinedBreakpoint userDefinedBreakpoint = bridge.getBreakpointAt(this.source, locationParser, this.sourceData.getRcEnd());
            handleBreakpoint(bridge, cx, userDefinedBreakpoint);
        }

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


    public Set<String> getLocalNames() {
        return localNames;
    }

    public Scriptable getScope() {
        return scope;
    }
}

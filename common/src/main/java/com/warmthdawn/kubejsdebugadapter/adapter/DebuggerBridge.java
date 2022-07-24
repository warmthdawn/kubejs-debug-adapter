package com.warmthdawn.kubejsdebugadapter.adapter;

import com.warmthdawn.kubejsdebugadapter.data.breakpoint.ScriptLocation;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.UserDefinedBreakpoint;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.BreakpointMeta;
import com.warmthdawn.kubejsdebugadapter.data.variable.IVariableTreeNode;
import com.warmthdawn.kubejsdebugadapter.debugger.BreakpointManager;
import com.warmthdawn.kubejsdebugadapter.debugger.DebugSession;
import com.warmthdawn.kubejsdebugadapter.utils.EvalUtils;
import com.warmthdawn.kubejsdebugadapter.utils.LocationParser;
import com.warmthdawn.kubejsdebugadapter.utils.PathUtil;
import com.warmthdawn.kubejsdebugadapter.utils.VariableUtils;
import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.Scriptable;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

public class DebuggerBridge {
    private final IDebugProtocolClient client;
    private final DataConverter converter;

    private final DebugSession session;

    public DebuggerBridge(IDebugProtocolClient client, DataConverter converter, DebugSession session) {
        this.client = client;
        this.converter = converter;
        this.session = session;
    }

    private final BreakpointManager breakpointManager = new BreakpointManager();

    public BreakpointManager getBreakpointManager() {
        return breakpointManager;
    }

    public UserDefinedBreakpoint getBreakpointAt(String source, LocationParser parser, int pos) {
        ScriptLocation location = parser.toLocation(pos);
        for (UserDefinedBreakpoint breakpoint : breakpointManager.getBreakpoints(source)) {
            if (breakpoint.getLine() == location.getLineNumber() && breakpoint.getColumn() == location.getColumnNumber()) {
                return breakpoint;
            }
        }

        return null;
    }

    public boolean hasFunctionBreakpointFor(String functionName) {
        return false;
    }

    public boolean shouldPause() {
        return false;
    }


    public void notifyThread(int threadId, boolean isStart) {
        ThreadEventArguments args = new ThreadEventArguments();
        args.setThreadId(threadId);
        if (isStart) {
            args.setReason(ThreadEventArgumentsReason.STARTED);
        } else {
            args.setReason(ThreadEventArgumentsReason.EXITED);
        }
        client.thread(args);
    }

    public void sendOutput(String output) {
        sendOutput(output, -1);
    }

    public void sendOutput(String output, int id) {
        OutputEventArguments args = new OutputEventArguments();
        Calendar calendar = Calendar.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("[debug] ");

        addTime(calendar, sb);
        sb.append(output);
        sb.append(System.lineSeparator());
        args.setOutput(sb.toString());
        args.setCategory(OutputEventArgumentsCategory.CONSOLE);

        if (id >= 0) {
            args.setVariablesReference(id);
        }
        client.output(args);
    }

    public void sendError(String output) {
        OutputEventArguments args = new OutputEventArguments();
        Calendar calendar = Calendar.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("[error] ");

        addTime(calendar, sb);
        sb.append(output);
        sb.append(System.lineSeparator());
        args.setOutput(sb.toString());
        args.setCategory(OutputEventArgumentsCategory.STDERR);

        client.output(args);
    }

    private void addTime(Calendar calendar, StringBuilder sb) {
        sb.append('[');

        if (calendar.get(Calendar.HOUR_OF_DAY) < 10) {
            sb.append('0');
        }

        sb.append(calendar.get(Calendar.HOUR_OF_DAY));
        sb.append(':');

        if (calendar.get(Calendar.MINUTE) < 10) {
            sb.append('0');
        }

        sb.append(calendar.get(Calendar.MINUTE));
        sb.append(':');

        if (calendar.get(Calendar.SECOND) < 10) {
            sb.append('0');
        }

        sb.append(calendar.get(Calendar.SECOND));
        sb.append(']');
        sb.append(' ');
    }

    public void notifyStop(int threadId, String reason) {
        StoppedEventArguments args = new StoppedEventArguments();
        args.setThreadId(threadId);
        args.setReason(reason);
        client.stopped(args);
    }

    public boolean shouldPauseOnException(Throwable ex) {
        return false;
    }


    public void notifySource(String sourceName) {

    }


    public void notifySourceCompiled(String sourceName) {
        Source dapSource = PathUtil.getDAPSource(sourceName);
        breakpointManager.fixBreakpoints(sourceName, b -> {
            BreakpointEventArguments args = new BreakpointEventArguments();
            Breakpoint breakpoint = converter.toDAPBreakpoint(dapSource, result -> {
                result.setVerified(true);
            }, b);
            args.setBreakpoint(breakpoint);
            args.setReason(BreakpointEventArgumentsReason.CHANGED);
            client.breakpoint(args);
        }, b -> {
            BreakpointEventArguments args = new BreakpointEventArguments();
            Breakpoint breakpoint = converter.toDAPBreakpoint(dapSource, result -> {
                result.setVerified(false);
                result.setMessage("Could not add breakpoint here");
            }, b);
            args.setBreakpoint(breakpoint);
            args.setReason(BreakpointEventArgumentsReason.REMOVED);
            client.breakpoint(args);
        });
    }


    public void sendLogs(String pack, String type, String line) {
        Calendar calendar = Calendar.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append('[')
            .append(pack)
            .append("] ");

        addTime(calendar, sb);
        sb.append('[');
        sb.append(type);
        sb.append(']');
        sb.append(' ');
        sb.append(line);
        sb.append(System.lineSeparator());
        OutputEventArguments args = new OutputEventArguments();
        args.setOutput(sb.toString());
        if ("ERR  ".equals(type)) {
            args.setCategory(OutputEventArgumentsCategory.STDERR);
        } else {
            args.setCategory(OutputEventArgumentsCategory.STDOUT);
        }
        client.output(args);

    }

    private static final Pattern LOGMESSAGE_PATTERN = Pattern.compile("\\{(.*)\\}");

    public void logMessage(ContextFactory factory, String logMessage, Scriptable scope) {
        String output = LOGMESSAGE_PATTERN.matcher(logMessage).replaceAll(it -> {
            try {
                Object result = EvalUtils.evaluate(factory, it.group(1), scope);
                return VariableUtils.variableToString(factory, result);
            } catch (Throwable t) {
                return t.getMessage();
            }
        });
        sendOutput(output);

    }
}

package com.warmthdawn.kubejsdebugadapter.adapter;

import com.warmthdawn.kubejsdebugadapter.data.UserDefinedBreakpoint;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.BreakpointMeta;
import com.warmthdawn.kubejsdebugadapter.debugger.BreakpointManager;
import com.warmthdawn.kubejsdebugadapter.utils.LocationParser;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;

import java.util.Calendar;

public class DebuggerBridge {
    private final IDebugProtocolClient client;

    public DebuggerBridge(IDebugProtocolClient client) {
        this.client = client;
    }

    private final BreakpointManager breakpointManager = new BreakpointManager();

    public BreakpointManager getBreakpointManager() {
        return breakpointManager;
    }

    public UserDefinedBreakpoint getBreakpointAt(String source, LocationParser parser, BreakpointMeta meta) {
        for (UserDefinedBreakpoint breakpoint : breakpointManager.getBreakpoints(source)) {
//            if (breakpoint.getLine() == lineNumber) {
//                return true;
//            }
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
        OutputEventArguments args = new OutputEventArguments();
        Calendar calendar = Calendar.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append("[debug] ");

        addTime(calendar, sb);
        sb.append(output);
        sb.append(System.lineSeparator());
        args.setOutput(sb.toString());
        args.setCategory(OutputEventArgumentsCategory.CONSOLE);
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

}

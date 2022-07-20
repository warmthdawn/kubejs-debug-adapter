package com.warmthdawn.kubejsdebugadapter.debugger;

import com.warmthdawn.kubejsdebugadapter.adapter.DataConverter;
import com.warmthdawn.kubejsdebugadapter.data.UserDefinedBreakpoint;
import com.warmthdawn.kubejsdebugadapter.utils.PathUtil;
import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.SourceBreakpoint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class BreakpointManager {
    private final Map<String, List<UserDefinedBreakpoint>> breakpoints = new ConcurrentHashMap<>();
    private final Map<Integer, UserDefinedBreakpoint> breakpointIdMap = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private int currentId;

    private int nextId() {
        lock.lock();
        try {
            return currentId++;
        } finally {
            lock.unlock();
        }
    }


    public void setBreakpoint(String sourceId, List<UserDefinedBreakpoint> breakpoints) {
        this.breakpoints.put(sourceId, breakpoints);
    }

    public List<UserDefinedBreakpoint> getBreakpoints(String sourceId) {
        List<UserDefinedBreakpoint> result = this.breakpoints.get(sourceId);
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }

    public List<Breakpoint> setBreakpoints(SetBreakpointsArguments args, DataConverter converter) {

        String sourceId = PathUtil.getSourceId(args.getSource());

        if (sourceId == null) {
            return Arrays.stream(args.getBreakpoints()).map(it -> {
                Breakpoint b = new Breakpoint();
                b.setSource(args.getSource());
                b.setColumn(it.getColumn());
                b.setLine(it.getLine());
                b.setVerified(false);
                b.setMessage("The script may not be in a valid script pack for kubejs");
                return b;
            }).collect(Collectors.toList());

        }

        List<UserDefinedBreakpoint> target = new ArrayList<>();
        for (SourceBreakpoint sourceBreakpoint : args.getBreakpoints()) {
            int id = nextId();
            UserDefinedBreakpoint scriptBreakpoint = converter.convertScriptBreakpoint(sourceBreakpoint, sourceId, nextId());
            breakpointIdMap.put(id, scriptBreakpoint);
            target.add(scriptBreakpoint);
        }

        setBreakpoint(sourceId, target);

        return converter.convertDAPBreakpoints(args.getSource(), target, b -> {
            b.setVerified(true);
        });

    }

}

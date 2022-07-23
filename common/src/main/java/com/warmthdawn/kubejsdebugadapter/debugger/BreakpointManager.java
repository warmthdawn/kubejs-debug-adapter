package com.warmthdawn.kubejsdebugadapter.debugger;

import com.warmthdawn.kubejsdebugadapter.adapter.DataConverter;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.UserDefinedBreakpoint;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.ScriptSourceData;
import com.warmthdawn.kubejsdebugadapter.utils.BreakpointUtils;
import com.warmthdawn.kubejsdebugadapter.utils.PathUtil;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.SourceBreakpoint;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BreakpointManager {
    private final Map<String, List<UserDefinedBreakpoint>> breakpoints = new ConcurrentHashMap<>();
    private final Map<Integer, UserDefinedBreakpoint> breakpointIdMap = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private int currentId;
    private SourceManager sourceManager;

    private int nextId() {
        lock.lock();
        try {
            return currentId++;
        } finally {
            lock.unlock();
        }
    }


    public IntSet setBreakpoint(String sourceId, List<UserDefinedBreakpoint> breakpoints) {
        if (sourceManager.isSourceLoaded(sourceId)) {
            ScriptSourceData data = sourceManager.getSourceData(sourceId);
            IntSet toRemove = new IntOpenHashSet();
            List<UserDefinedBreakpoint> validBreakpoints = BreakpointUtils.coerceBreakpoints(
                data,
                breakpoints,
                cb -> {
                },
                rb -> {
                    breakpointIdMap.remove(rb.getId());
                    toRemove.add(rb.getId());
                }
            );
            this.breakpoints.put(sourceId, validBreakpoints);
            return toRemove;

        } else {
            this.breakpoints.put(sourceId, breakpoints);
            return IntSets.emptySet();
        }

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

        IntSet toRemove = setBreakpoint(sourceId, target);
        if (sourceManager.isSourceLoaded(sourceId)) {
            return converter.convertDAPBreakpoints(args.getSource(), target, b -> {
                if (toRemove.contains(b.getId().intValue())) {
                    b.setVerified(false);
                    b.setMessage("Can not add breakpoint");
                } else {
                    b.setVerified(true);
                }
            });
        } else {
            return converter.convertDAPBreakpoints(args.getSource(), target, b -> {
                b.setVerified(false);
                b.setMessage("The script is not loaded yet");
            });
        }

    }

    public void setSourceManager(SourceManager sourceManager) {
        this.sourceManager = sourceManager;
    }


    public void fixBreakpoints(String sourceId, Consumer<UserDefinedBreakpoint> updateAction, Consumer<UserDefinedBreakpoint> removeAction) {
        ScriptSourceData data = sourceManager.getSourceData(sourceId);
        List<UserDefinedBreakpoint> breakpoints = this.breakpoints.get(sourceId);
        if (breakpoints == null) {
            return;
        }

        List<UserDefinedBreakpoint> validBreakpoints = BreakpointUtils.coerceBreakpoints(
            data,
            breakpoints,
            updateAction,
            (rb) -> {
                breakpointIdMap.remove(rb.getId());
                removeAction.accept(rb);
            }
        );

        this.breakpoints.put(sourceId, validBreakpoints);
    }
}

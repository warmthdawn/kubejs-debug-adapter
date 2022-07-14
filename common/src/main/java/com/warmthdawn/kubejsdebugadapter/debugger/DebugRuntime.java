package com.warmthdawn.kubejsdebugadapter.debugger;

import com.warmthdawn.kubejsdebugadapter.adapter.DebuggerBridge;
import dev.latvian.mods.rhino.Context;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class DebugRuntime {
    private static final DebugRuntime _instance = new DebugRuntime();

    public static DebugRuntime getInstance() {
        return _instance;
    }


    private volatile DebuggerBridge bridge;

    private volatile boolean configurationDone = false;

    private final Map<Integer, DebugThread> threadMap = new ConcurrentHashMap<>();
    private final Map<Context, Integer> contextThreadMap = new ConcurrentHashMap<>();


    private int threadId = 0;
    private int frameId = 0;
    private final ReentrantLock idLock = new ReentrantLock();

    public int nextThreadId() {
        idLock.lock();
        try {
            return threadId++;
        } finally {
            idLock.unlock();
        }
    }

    public int nextFrameId() {
        idLock.lock();
        try {
            return frameId++;
        } finally {
            idLock.unlock();
        }
    }

    public DebuggerBridge getBridge() {
        return bridge;
    }

    public DebugThread newThread(Context cx, String name) {
        int id = nextThreadId();
        DebugThread thread = new DebugThread(id, this, cx, name);
        threadMap.put(id, thread);
        contextThreadMap.put(cx, id);
        return thread;
    }

    public DebugThread getThread(int id) {
        return threadMap.get(id);
    }

    public DebugThread getThread(Context cx) {
        return getThread(contextThreadMap.get(cx));
    }

    public DebugThread removeThread(int id) {
        return threadMap.remove(id);
    }

    public DebugThread removeThread(Context cx) {
        Integer id = contextThreadMap.remove(cx);
        if (id != null) {
            return removeThread(id);
        }
        return null;
    }

    public Collection<DebugThread> getThreads() {
        return threadMap.values();
    }

    public void resumeAll() {
        for (DebugThread thread : threadMap.values()) {
            thread.resume();
        }
    }

    public void setBridge(DebuggerBridge bridge) {
        this.bridge = bridge;
    }

    public void removeBridge() {
        this.bridge = null;
    }


    public boolean isConfigurationDone() {
        return configurationDone;
    }

    public void setConfigurationDone() {
        configurationDone = true;
    }
}

package com.warmthdawn.kubejsdebugadapter.debugger;

import dev.latvian.mods.rhino.Context;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class DebugThread {
    private final int id;
    private final DebugRuntime runtime;
    private final String name;

    private final Semaphore semaphore = new Semaphore(0);

    private final DebugContextData contextData;

    private volatile boolean isInterrupted = false;

    public DebugThread(int id, DebugRuntime runtime, Context cx, String name) {
        this.id = id;
        this.runtime = runtime;
        this.name = name;
        this.contextData =  DebugContextData.get(cx);
    }

    public void interrupt() {
        isInterrupted = true;
        try {
            semaphore.acquire();
        } catch (InterruptedException ignored) {

        }
    }

    public int id() {
        return id;
    }

    public KubeStackFrame[] stackFrames() {
        return contextData.stackFrames();
    }

    public String getName() {
        return this.name;
    }

    public void stepInto() {
        if(!isInterrupted) {
            return;
        }
        contextData.stepInto();
        resume();
    }

    public void stepOver() {
        if(!isInterrupted) {
            return;
        }
        contextData.stepOver();
        resume();
    }

    public void stepOut() {
        if(!isInterrupted) {
            return;
        }
        contextData.stepOut();
        resume();
    }

    public void resume() {
        if(!isInterrupted) {
            return;
        }
        isInterrupted = false;
        semaphore.release();
    }

    public void pause() {
        if(isInterrupted) {
            return;
        }
        contextData.pause();
    }

}

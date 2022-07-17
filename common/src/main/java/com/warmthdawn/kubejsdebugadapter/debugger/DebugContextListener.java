package com.warmthdawn.kubejsdebugadapter.debugger;

import com.warmthdawn.kubejsdebugadapter.adapter.DebuggerBridge;
import com.warmthdawn.kubejsdebugadapter.api.IDebuggableContext;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextFactory;

public class DebugContextListener implements ContextFactory.Listener {
    private final String managerName;

    public DebugContextListener(String managerName) {
        this.managerName = managerName;
    }

    @Override
    public void contextCreated(Context cx) {
        if (!(cx instanceof IDebuggableContext)) {
            return;
        }
        DebugRuntime runtime = DebugRuntime.getInstance();

        ((IDebuggableContext) cx).setDebugger(new DebuggerProxy(runtime, managerName), new DebugContextData());
        String threadName = managerName + ":" + Thread.currentThread().getName();

        DebugThread debugThread = runtime.newThread(cx, threadName);
        DebuggerBridge bridge = runtime.getBridge();
        if (bridge != null) {
            bridge.notifyThread(debugThread.id(), true);
        }


    }

    @Override
    public void contextReleased(Context cx) {
        DebugRuntime runtime = DebugRuntime.getInstance();

        DebugThread debugThread = runtime.removeThread(cx);
        DebuggerBridge bridge = runtime.getBridge();
        if (bridge != null && debugThread != null) {
            bridge.notifyThread(debugThread.id(), false);
        }

    }
}

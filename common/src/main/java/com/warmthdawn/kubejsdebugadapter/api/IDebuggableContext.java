package com.warmthdawn.kubejsdebugadapter.api;

public interface IDebuggableContext {

    /**
     * Return the current debugger.
     *
     * @return the debugger, or null if none is attached.
     */
    Debugger getDebugger();


    /**
     * Return the debugger context data associated with current context.
     *
     * @return the debugger data, or null if debugger is not attached
     */
    Object getDebuggerContextData();


    /**
     * Set the associated debugger.
     *
     * @param debugger the debugger to be used on callbacks from the engine.
     * @param contextData arbitrary object that debugger can use to store per Context data.
     */
    void setDebugger(Debugger debugger, Object contextData);



}

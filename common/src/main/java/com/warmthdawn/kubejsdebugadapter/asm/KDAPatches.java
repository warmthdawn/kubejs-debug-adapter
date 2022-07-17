package com.warmthdawn.kubejsdebugadapter.asm;

import com.warmthdawn.kubejsdebugadapter.api.*;
import com.warmthdawn.kubejsdebugadapter.utils.ExtendedIcode;
import dev.latvian.mods.rhino.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.latvian.mods.rhino.UniqueTag.DOUBLE_MARK;

public class KDAPatches {
    public static boolean processExtraOp(Context cx, int op, Object frameObj) {
        if (frameObj instanceof IDebuggableCallFrame frame) {
            if (op == ExtendedIcode.Icode_BREAKPOINT) {
                if (frame.getDebuggerFrame() != null) {
                    int line = frame.readBreakpointLocation();
                    frame.getDebuggerFrame().onPossibleBreakpoint(cx, line);
                }
                frame.increasePC(2);
                return true;
            }

        }
        return false;
    }

    public static void initDebugFrame(Object callFrame, Context cx, Object fnOrScript) {
        if (cx instanceof IDebuggableContext && fnOrScript instanceof IDebuggableScriptProvider && callFrame instanceof IDebuggableCallFrame) {
            Debugger debugger = ((IDebuggableContext) cx).getDebugger();
            if (debugger != null) {
                ((IDebuggableCallFrame) callFrame).setDebuggerFrame(debugger.getFrame(cx, ((IDebuggableScriptProvider) fnOrScript).getDebuggableScript()));
            }
        }
    }


    private static void notifyDebugger_r(IDebuggableContext cx, DebuggableScript dscript, String debugSource) {
        cx.getDebugger().handleCompilationDone((Context) cx, dscript, debugSource);
        for (int i = 0; i != dscript.getFunctionCount(); ++i) {
            notifyDebugger_r(cx, dscript.getFunction(i), debugSource);
        }
    }

    public static void debugScriptComplied(Context cx, Object bytecode, String sourceString) {
        Debugger debugger = ((IDebuggableContext) cx).getDebugger();
        if (debugger != null) {
            if (sourceString == null) Kit.codeBug();
            if (bytecode instanceof IDebuggableScriptProvider) {
                DebuggableScript dscript = ((IDebuggableScriptProvider) bytecode).getDebuggableScript();
                notifyDebugger_r((IDebuggableContext) cx, dscript, sourceString);
            } else {
                throw new RuntimeException("NOT SUPPORTED");
            }
        }
    }

    private static void enterDebugFrame(Context cx, Object frameObj, Object[] args, boolean continuationRestart) {
        if (frameObj instanceof IDebuggableCallFrame frame && frame.getDebuggerFrame() != null) {

            Scriptable scope = frame.getScope();
            if (scope == null) {
                Kit.codeBug();
            } else if (continuationRestart) {
                // Walk the parent chain of frame.scope until a NativeCall is
                // found. Normally, frame.scope is a NativeCall when called
                // from initFrame() for a debugged or activatable function.
                // However, when called from interpretLoop() as part of
                // restarting a continuation, it can also be a NativeWith if
                // the continuation was captured within a "with" or "catch"
                // block ("catch" implicitly uses NativeWith to create a scope
                // to expose the exception variable).
                for (; ; ) {
                    if (scope instanceof NativeWith) {
                        scope = scope.getParentScope();
                        if (scope == null
                            || (frame.getParent() != null
                            && frame.getParent().getScope() == scope)) {
                            // If we get here, we didn't find a NativeCall in
                            // the call chain before reaching parent frame's
                            // scope. This should not be possible.
                            Kit.codeBug();
                            break; // Never reached, but keeps the static analyzer
                            // happy about "scope" not being null 5 lines above.
                        }
                    } else {
                        break;
                    }
                }
            }
            frame.getDebuggerFrame().onEnter(cx, scope, frame.getThisObj(), args);

        }
    }

    private static void exitDebugFrame(Context cx, Object frameObj, Object throwable) {

        if (frameObj instanceof IDebuggableCallFrame frame && frame.getDebuggerFrame() != null) {
            try {
                if (throwable instanceof Throwable) {
                    frame.getDebuggerFrame().onExit(cx, true, throwable);
                } else {
                    Object result;
                    if (throwable instanceof IResultProvider provider) {
                        result = provider.getResult();
                    } else {
                        result = frame.getResult();
                    }
                    if (result == DOUBLE_MARK) {
                        double resultDbl;

                        if (throwable instanceof IResultProvider provider) {
                            resultDbl = provider.getResultDbl();
                        } else {
                            resultDbl = frame.getResultDbl();
                        }
                        result = ScriptRuntime.wrapNumber(resultDbl);
                    }
                    frame.getDebuggerFrame().onExit(cx, false, result);
                }
            } catch (Throwable ex) {
                System.err.println("RHINO USAGE WARNING: onExit terminated with exception");
                ex.printStackTrace(System.err);
            }
        }
    }

}

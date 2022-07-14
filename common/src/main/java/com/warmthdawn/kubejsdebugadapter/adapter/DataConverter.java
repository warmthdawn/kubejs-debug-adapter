package com.warmthdawn.kubejsdebugadapter.adapter;

import com.warmthdawn.kubejsdebugadapter.data.*;
import com.warmthdawn.kubejsdebugadapter.debugger.DebugSession;
import com.warmthdawn.kubejsdebugadapter.debugger.DebugThread;
import com.warmthdawn.kubejsdebugadapter.debugger.KubeStackFrame;
import com.warmthdawn.kubejsdebugadapter.utils.PathUtil;
import org.eclipse.lsp4j.debug.*;
import org.eclipse.lsp4j.debug.Thread;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class DataConverter {
    private boolean linesStartAt1;

    public DataConverter(boolean linesStartAt1) {
        this.linesStartAt1 = linesStartAt1;
    }

    public int toDAPLineNumber(int lineNumber) {
        return linesStartAt1 ? lineNumber : lineNumber - 1;
    }


    public int toKubeLineNumber(int lineNumber) {
        return linesStartAt1 ? lineNumber : lineNumber + 1;
    }


    public List<Breakpoint> convertDAPBreakpoints(Source source, List<ScriptBreakpoint> breakpoints, Consumer<Breakpoint> consumer) {
        List<Breakpoint> result = new ArrayList<>();
        for (ScriptBreakpoint breakpoint : breakpoints) {
            Breakpoint b = toDAPBreakpoint(source, consumer, breakpoint);
            result.add(b);
        }
        return result;
    }

    @NotNull
    public Breakpoint toDAPBreakpoint(Source source, Consumer<Breakpoint> consumer, ScriptBreakpoint breakpoint) {
        Breakpoint b = new Breakpoint();
        b.setLine(toDAPLineNumber(breakpoint.getLine()));
        b.setSource(source);
        consumer.accept(b);
        return b;
    }

    public ScriptBreakpoint convertScriptBreakpoint(SourceBreakpoint breakpoint, String source, int id) {
        ScriptBreakpoint result = new ScriptBreakpoint();

        result.setLine(toKubeLineNumber(breakpoint.getLine()));
        return result;
    }


    public StackFrame[] toDAPStackFrames(KubeStackFrame[] kubeStackFrames, DebugSession session) {
        StackFrame[] result = new StackFrame[kubeStackFrames.length];
        for (int i = 0; i < kubeStackFrames.length; i++) {
            KubeStackFrame kubeStackFrame = kubeStackFrames[i];
            StackFrame stackFrame = new StackFrame();
            session.addStackFrame(kubeStackFrame);
            stackFrame.setId(kubeStackFrame.getId());
            stackFrame.setSource(PathUtil.getDAPSource(kubeStackFrame.getSource()));
            stackFrame.setLine(toDAPLineNumber(kubeStackFrame.currentLine()));
            result[i] = stackFrame;
        }

        return result;
    }

    public Thread[] toDAPThread(Collection<DebugThread> threads) {
        List<Thread> result = new ArrayList<>();
        for (DebugThread thread : threads) {
            Thread t = new Thread();
            t.setId(thread.id());
            t.setName(thread.getName());
            result.add(t);
        }
        return result.toArray(new Thread[0]);
    }

    public Variable toDAPVariable(IVariableTreeNode variable) {
        Variable result = new Variable();
        result.setName(variable.getName());
        result.setVariablesReference(variable.getId());
        if (variable instanceof KubeVariable) {
            result.setValue(((KubeVariable) variable).getValue());
            result.setType(((KubeVariable) variable).getType());
        }
        if(variable instanceof ErrorVariable) {
            result.setValue(((ErrorVariable) variable).getValue());
        }

        return result;
    }

    public Scope[] toDAPScopes(List<VariableScope> scopes) {
        Scope[] result = new Scope[scopes.size()];
        for (int i = 0; i < scopes.size(); i++) {
            VariableScope scope = scopes.get(i);
            Scope s = new Scope();
            s.setName(scope.getName());
            s.setVariablesReference(scope.getId());
            s.setPresentationHint(scope.getPresentationHint());
            result[i] = s;
        }
        return result;
    }
}

package com.warmthdawn.kubejsdebugadapter.adapter;

import com.ibm.icu.impl.Pair;
import com.warmthdawn.kubejsdebugadapter.data.*;
import com.warmthdawn.kubejsdebugadapter.data.variable.ErrorVariable;
import com.warmthdawn.kubejsdebugadapter.data.variable.IVariableTreeNode;
import com.warmthdawn.kubejsdebugadapter.data.variable.KubeVariable;
import com.warmthdawn.kubejsdebugadapter.data.variable.VariableScope;
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
    private final boolean linesStartAt1;
    private final boolean columnsStartAt1;

    public DataConverter(boolean linesStartAt1, boolean columnsStartAt1) {
        this.linesStartAt1 = linesStartAt1;
        this.columnsStartAt1 = columnsStartAt1;
    }

    public int toDAPLineNumber(int lineNumber) {
        return linesStartAt1 ? lineNumber + 1 : lineNumber;
    }

    public int toDAPColumnNumber(int columnNumber) {
        return columnsStartAt1 ? columnNumber + 1 : columnNumber;
    }

    public int toKubeLineNumber(int lineNumber) {
        return linesStartAt1 ? lineNumber - 1 : lineNumber;
    }

    public int toKubeColumnNumber(int columnNumber) {
        return columnsStartAt1 ? columnNumber - 1 : columnNumber;
    }


    public List<Breakpoint> convertDAPBreakpoints(Source source, List<UserDefinedBreakpoint> breakpoints, Consumer<Breakpoint> consumer) {
        List<Breakpoint> result = new ArrayList<>();
        for (UserDefinedBreakpoint breakpoint : breakpoints) {
            Breakpoint b = toDAPBreakpoint(source, consumer, breakpoint);
            result.add(b);
        }
        return result;
    }

    @NotNull
    public Breakpoint toDAPBreakpoint(Source source, Consumer<Breakpoint> consumer, UserDefinedBreakpoint breakpoint) {
        Breakpoint b = new Breakpoint();
        b.setLine(toDAPLineNumber(breakpoint.getLine()));
        b.setColumn(toDAPColumnNumber(breakpoint.getColumn()));
        b.setSource(source);
        b.setId(breakpoint.getId());
        consumer.accept(b);
        return b;
    }

    public UserDefinedBreakpoint convertScriptBreakpoint(SourceBreakpoint breakpoint, String source, int id) {
        UserDefinedBreakpoint result = new UserDefinedBreakpoint();

        result.setLine(toKubeLineNumber(breakpoint.getLine()));
        if (breakpoint.getColumn() != null) {
            result.setColumn(toKubeLineNumber(breakpoint.getColumn()));
        }
        result.setId(id);
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
            stackFrame.setLine(toDAPLineNumber(kubeStackFrame.getLine()));
            stackFrame.setColumn(toDAPColumnNumber(kubeStackFrame.getColumn()));
            stackFrame.setEndLine(toDAPLineNumber(kubeStackFrame.getEndLine()));
            stackFrame.setEndColumn(toDAPColumnNumber(kubeStackFrame.getEndColumn()));

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
        if (variable instanceof ErrorVariable) {
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

    public BreakpointLocation[] toDAPBreakpointLocations(List<Pair<ScriptLocation, ScriptLocation>> locations) {
        List<BreakpointLocation> result = new ArrayList<>(locations.size());
        for (Pair<ScriptLocation, ScriptLocation> location : locations) {
            BreakpointLocation b = new BreakpointLocation();
            b.setLine(toDAPLineNumber(location.first.getLineNumber()));
            b.setColumn(toDAPColumnNumber(location.first.getColumnNumber()));
            b.setEndLine(toDAPLineNumber(location.second.getLineNumber()));
            b.setEndColumn(toDAPColumnNumber(location.second.getColumnNumber()));
            result.add(b);
        }
        return result.toArray(BreakpointLocation[]::new);
    }
}

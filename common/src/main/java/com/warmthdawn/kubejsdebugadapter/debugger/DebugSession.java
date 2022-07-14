package com.warmthdawn.kubejsdebugadapter.debugger;

import com.warmthdawn.kubejsdebugadapter.data.ErrorVariable;
import com.warmthdawn.kubejsdebugadapter.utils.VariableUtils;
import com.warmthdawn.kubejsdebugadapter.data.IVariableTreeNode;
import com.warmthdawn.kubejsdebugadapter.data.KubeVariable;
import com.warmthdawn.kubejsdebugadapter.data.VariableScope;
import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.Undefined;
import org.eclipse.lsp4j.debug.ScopePresentationHint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class DebugSession {
    private Map<Integer, KubeStackFrame> stackFramePool = new HashMap<>();
    private Map<Integer, IVariableTreeNode> variablePool = new HashMap();

    private int variableId;

    private int nextVariableId() {
        return variableId++;
    }

    public KubeStackFrame getStackFrame(int id) {
        return stackFramePool.get(id);
    }

    public void addStackFrame(KubeStackFrame frame) {
        stackFramePool.put(frame.getId(), frame);
    }


    public void addVariable(IVariableTreeNode variable) {
        variablePool.put(variable.getId(), variable);
    }

    public IVariableTreeNode getVariable(int id) {
        return variablePool.get(id);
    }

    public void clearPool() {
        stackFramePool.clear();
    }


    public VariableScope createVariableScope(String name, Consumer<VariableScope.Builder> childrenProvider, String presentationHint) {
        VariableScope result = new VariableScope(name, presentationHint, nextVariableId(), childrenProvider);
        addVariable(result);
        return result;
    }

    public KubeVariable createVariable(Object variable, String name, ContextFactory factory) {
        KubeVariable result = new KubeVariable(variable, nextVariableId(), name, factory);
        addVariable(result);
        return result;
    }


    public ErrorVariable createError(Throwable cause, String name, ContextFactory factory) {
        ErrorVariable result = new ErrorVariable(nextVariableId(), cause, name, factory);
        addVariable(result);
        return result;
    }

    public List<VariableScope> getFrameVariables(KubeStackFrame frame) {
        ContextFactory factory = frame.getFactory();
        List<VariableScope> variables = new ArrayList<>();

        variables.add(createVariableScope("arguments", builder -> {
            Object[] args = frame.getArgs();
            String[] paramNames = frame.getParamNames();
            for (int i = 0; i < paramNames.length; i++) {
                if (i < args.length) {
                    builder.addChild(createVariable(args[i], paramNames[i], factory));
                } else {
                    builder.addChild(createVariable(Undefined.instance, paramNames[i], factory));
                }
            }
        }, ScopePresentationHint.ARGUMENTS));

        variables.add(createVariableScope("locals", builder -> {
            if(frame.getThisObj() != null && frame.getThisObj() != Undefined.instance) {
                builder.addChild(createVariable(frame.getThisObj(), "this", factory));
            }
            Scriptable scope = frame.getScope();
            Object[] objectIds = VariableUtils.getObjectIds(factory, scope);
            for (Object objectId : objectIds) {
                Object object = VariableUtils.getObjectProperty(factory, scope, objectId);
                String name = VariableUtils.variableToString(factory, objectId);
                builder.addChild(createVariable(object, name, factory));
            }
        }, ScopePresentationHint.LOCALS));



        return variables;
    }
}

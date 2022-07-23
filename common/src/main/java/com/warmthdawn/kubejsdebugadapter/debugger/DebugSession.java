package com.warmthdawn.kubejsdebugadapter.debugger;

import com.warmthdawn.kubejsdebugadapter.data.variable.*;
import com.warmthdawn.kubejsdebugadapter.utils.PathUtil;
import com.warmthdawn.kubejsdebugadapter.utils.PropertyUtils;
import com.warmthdawn.kubejsdebugadapter.utils.Utils;
import com.warmthdawn.kubejsdebugadapter.utils.VariableUtils;
import dev.latvian.mods.kubejs.script.ScriptPack;
import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.Undefined;
import org.eclipse.lsp4j.debug.ScopePresentationHint;

import java.util.*;
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
        VariableDescriptor descriptor = VariableDescriptor.createVar(name);
        return createVariable(variable, name, factory, descriptor);
    }

    public IVariableTreeNode resolveAndCreateVariable(Scriptable parent, Object objectId, ContextFactory factory) {
        String key = VariableUtils.variableToString(factory, objectId);
        try {
            VariableDescriptor descriptor = PropertyUtils.getDescriptor(factory, parent, objectId);

            if (descriptor.isLazy()) {
                return createLazy(parent, objectId, key, factory, descriptor);
            } else {
                Object objectProperty = Utils.timeoutWith(2000, () ->
                    VariableUtils.getObjectProperty(factory, parent, objectId));

                return createVariable(objectProperty, key, factory, descriptor);
            }


        } catch (Throwable e) {
            return createError(e, key, factory);
        }
    }


    public KubeVariable createVariable(Object variable, String name, ContextFactory factory, VariableDescriptor descriptor) {
        KubeVariable result = new KubeVariable(variable, nextVariableId(), name, factory, descriptor);
        addVariable(result);
        return result;
    }

    public LazyVariable createLazy(Object parent, Object objectId, String name, ContextFactory factory, VariableDescriptor descriptor) {
        LazyVariable result = new LazyVariable(nextVariableId(), name, parent, objectId, factory, descriptor);
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

        variables.add(createVariableScope("Arguments", builder -> {
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


        variables.add(createVariableScope("Locals", builder -> {
            if (frame.getThisObj() != null && frame.getThisObj() != Undefined.instance) {
                builder.addChild(createVariable(frame.getThisObj(), "this", factory));
            }


            Scriptable scope = frame.getScope();
            Object[] objectIds = VariableUtils.getObjectIds(factory, scope);
            Set<String> localNames = frame.getLocalNames();
            for (Object objectId : objectIds) {
                if (!(objectId instanceof String)) {
                    continue;
                }
                if (!localNames.contains(objectId)) {
                    continue;
                }

                builder.addChild(resolveAndCreateVariable(scope, objectId, factory));
            }
        }, ScopePresentationHint.LOCALS));


        // 全局：
        variables.add(createVariableScope("Globals", builder -> {
            ScriptPack scriptPack = PathUtil.getScriptPack(frame.getSource());
            Scriptable global = scriptPack.scope;
            Object[] objectIds = VariableUtils.getObjectIds(factory, global);
            for (Object objectId : objectIds) {
                builder.addChild(resolveAndCreateVariable(global, objectId, factory));
            }
        }, ScopePresentationHint.LOCALS));

        return variables;
    }
}

package com.warmthdawn.kubejsdebugadapter.data.variable;

import com.warmthdawn.kubejsdebugadapter.api.DebuggableScript;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.FunctionSourceData;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.ScriptSourceData;
import com.warmthdawn.kubejsdebugadapter.debugger.DebugRuntime;
import com.warmthdawn.kubejsdebugadapter.debugger.DebugSession;
import com.warmthdawn.kubejsdebugadapter.utils.*;
import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.Scriptable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KubeVariable implements IVariableTreeNode {
    private static final Logger log = LogManager.getLogger();
    private Object obj;


    private String value;
    private int id;
    private String name;
    private String type;
    private ContextFactory factory;
    private VariableDescriptor descriptor;

    public KubeVariable(Object variable, int id, String name, ContextFactory factory, @NotNull VariableDescriptor descriptor) {
        this.obj = variable;
        this.id = id;
        this.name = name;
        this.factory = factory;
        this.descriptor = descriptor;
    }


    public String getValue() {
        if (value == null) {
            value = VariableUtils.variableValue(factory, obj);
        }
        return value;
    }

    public String getType() {
        if (type == null) {
            type = VariableUtils.variableType(factory, obj);
        }
        return type;
    }

    public VariableDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<IVariableTreeNode> getChildren(DebugSession session) {
        if (!VariableUtils.shouldShowChildren(factory, obj)) {
            return Collections.emptyList();
        }
        if(!(obj instanceof Scriptable scriptable)) {
            return Collections.emptyList();
        }

        Object[] objectIds = VariableUtils.getObjectIds(factory, obj);
        List<IVariableTreeNode> children = new ArrayList<>();
        for (Object objectId : objectIds) {
            IVariableTreeNode child = session.resolveAndCreateVariable(scriptable, objectId, factory);
            children.add(child);
        }
        return children;
    }
}

package com.warmthdawn.kubejsdebugadapter.data.variable;

import com.warmthdawn.kubejsdebugadapter.debugger.DebugSession;
import com.warmthdawn.kubejsdebugadapter.utils.VariableUtils;
import dev.latvian.mods.rhino.ContextFactory;

import java.util.Collections;
import java.util.List;

public class LazyVariable implements IVariableTreeNode {

    private int id;
    private String name;

    private Object parent;
    private Object objectId;
    private ContextFactory factory;
    private VariableDescriptor descriptor;

    public LazyVariable(int id, String name, Object parent, Object objectId, ContextFactory factory, VariableDescriptor descriptor) {
        this.id = id;
        this.name = name;
        this.parent = parent;
        this.objectId = objectId;
        this.factory = factory;
        this.descriptor = descriptor;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public VariableDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public List<IVariableTreeNode> getChildren(DebugSession session) {
        return Collections.singletonList(session.createVariable(
            VariableUtils.getObjectProperty(factory, parent, objectId),
            VariableUtils.variableToString(factory, objectId),
            factory));
    }
}

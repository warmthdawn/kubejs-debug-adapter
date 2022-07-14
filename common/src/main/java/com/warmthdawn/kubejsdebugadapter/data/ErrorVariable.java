package com.warmthdawn.kubejsdebugadapter.data;

import com.warmthdawn.kubejsdebugadapter.debugger.DebugSession;
import dev.latvian.mods.rhino.ContextFactory;

import java.util.Collections;
import java.util.List;

public class ErrorVariable implements IVariableTreeNode {
    private final Throwable cause;
    private final String name;
    private final int id;
    private final ContextFactory factory;

    public ErrorVariable(int id, Throwable cause, String name, ContextFactory factory) {
        this.cause = cause;
        this.name = name;
        this.id = id;
        this.factory = factory;
    }

    @Override
    public int getId() {
        return id;
    }

    public String getValue() {
        return "Could not get variable: " + cause.getMessage();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<IVariableTreeNode> getChildren(DebugSession session) {
        return Collections.singletonList(session.createVariable(cause, "cause", factory));
    }
}

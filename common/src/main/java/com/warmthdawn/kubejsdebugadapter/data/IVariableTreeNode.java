package com.warmthdawn.kubejsdebugadapter.data;

import com.warmthdawn.kubejsdebugadapter.debugger.DebugSession;

import java.util.List;

public interface IVariableTreeNode {
    int getId();
    String getName();
    List<IVariableTreeNode> getChildren(DebugSession session);
}

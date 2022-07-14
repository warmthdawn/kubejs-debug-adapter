package com.warmthdawn.kubejsdebugadapter.data;

import com.warmthdawn.kubejsdebugadapter.debugger.DebugSession;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class VariableScope implements IVariableTreeNode {
    private final String name;

    private final String presentationHint;
    private int id;
    private List<IVariableTreeNode> children;
    private final Consumer<Builder> childrenProvider;

    public VariableScope(String name, String presentationHint, int id, Consumer<Builder> childrenProvider) {
        this.name = name;
        this.presentationHint = presentationHint;
        this.id = id;
        this.childrenProvider = childrenProvider;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getPresentationHint() {
        return presentationHint;
    }

    @Override
    public List<IVariableTreeNode> getChildren(DebugSession session) {
        if (children == null) {
            Builder builder = new Builder();
            childrenProvider.accept(builder);
            children = builder.build();
        }
        return children;
    }


    public static class Builder {
        private final List<IVariableTreeNode> children = new ArrayList<>();


        public Builder addChild(IVariableTreeNode child) {
            children.add(child);
            return this;
        }

        private List<IVariableTreeNode> build() {
            return children;
        }
    }

}

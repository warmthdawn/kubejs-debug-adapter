package com.warmthdawn.kubejsdebugadapter.data.breakpoint;


import dev.latvian.mods.rhino.ast.FunctionNode;
import dev.latvian.mods.rhino.ast.ScriptNode;

import java.util.ArrayList;
import java.util.List;

public class FunctionSourceData {
    private final int id;
    private final String name;
    private final int position;
    private final int length;


    public FunctionSourceData(ScriptNode scriptNode, int id) {
        position = scriptNode.getAbsolutePosition();
        length = scriptNode.getLength();
        if (scriptNode instanceof FunctionNode functionNode) {
            name = functionNode.getName();
        } else {
            name = scriptNode.getSourceName();
        }
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public int getLength() {
        return length;
    }

    private final List<BreakpointMeta> expressionBreakpointMetas = new ArrayList<>();
    private final List<StatementBreakpointMeta> statementBreakpointMetas = new ArrayList<>();


    public BreakpointMeta addExpressionBreakpointMeta(int position, int length) {
        int id = expressionBreakpointMetas.size();
        BreakpointMeta meta = new BreakpointMeta(id, position, length);
        expressionBreakpointMetas.add(meta);
        return meta;
    }

    public StatementBreakpointMeta addStatementBreakpointMeta(int position, int length, boolean mustBreak) {
        int id = statementBreakpointMetas.size();
        StatementBreakpointMeta meta = new StatementBreakpointMeta(id, position, length, mustBreak);
        statementBreakpointMetas.add(meta);
        return meta;
    }

    public BreakpointMeta getExpressionBreakpointMeta(int meta) {
        return expressionBreakpointMetas.get(meta);
    }

    public StatementBreakpointMeta getStatementBreakpointMeta(int meta) {
        return statementBreakpointMetas.get(meta);
    }


}

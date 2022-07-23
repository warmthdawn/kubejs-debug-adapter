package com.warmthdawn.kubejsdebugadapter.data.breakpoint;


import dev.latvian.mods.rhino.ast.Block;
import dev.latvian.mods.rhino.ast.FunctionNode;
import dev.latvian.mods.rhino.ast.ScriptNode;

import java.util.ArrayList;
import java.util.List;

public class FunctionSourceData {
    private final int id;
    private final String name;
    private final int position;
    private final int length;

    private final boolean hasBlock;

    private int lcStart = -1;

    private int rcEnd = -1;


    public FunctionSourceData(ScriptNode scriptNode, int id) {
        position = scriptNode.getAbsolutePosition();
        length = scriptNode.getLength();
        if (scriptNode instanceof FunctionNode functionNode) {
            name = functionNode.getName();
            if (functionNode.isExpressionClosure()) {
                hasBlock = false;
            } else {
                hasBlock = true;
                Block body = (Block) functionNode.getBody();
                int bodyPos = body.getAbsolutePosition();
                lcStart = bodyPos;
                rcEnd = bodyPos + body.getLength() - 1;
            }
        } else {
            name = scriptNode.getSourceName();
            hasBlock = false;
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

    public boolean isHasBlock() {
        return hasBlock;
    }

    public int getLcStart() {
        return lcStart;
    }

    public int getRcEnd() {
        return rcEnd;
    }

    private final List<BreakpointMeta> expressionBreakpointMetas = new ArrayList<>();
    private final List<StatementBreakpointMeta> statementBreakpointMetas = new ArrayList<>();


    public BreakpointMeta addExpressionBreakpointMeta(int position, int length, boolean lowPriority) {
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

    public List<StatementBreakpointMeta> getStatementBreakpointMetas() {
        return statementBreakpointMetas;
    }

    public List<BreakpointMeta> getExpressionBreakpointMetas() {
        return expressionBreakpointMetas;
    }

}

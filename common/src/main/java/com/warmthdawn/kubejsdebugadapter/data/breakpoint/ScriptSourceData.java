package com.warmthdawn.kubejsdebugadapter.data.breakpoint;

import com.warmthdawn.kubejsdebugadapter.utils.LocationParser;
import dev.latvian.mods.rhino.ast.FunctionNode;

import java.util.ArrayList;
import java.util.List;

public class ScriptSourceData {
    private String id;
    private String sourceString;


    private final List<FunctionSourceData> functions = new ArrayList<>();

    public ScriptSourceData(String sourceId) {
        this.id = sourceId;
    }

    public FunctionSourceData addFunction(FunctionNode functionNode) {
        int id = functions.size();
        FunctionSourceData functionSourceData = new FunctionSourceData(functionNode, id);
        functions.add(functionSourceData);
        return functionSourceData;
    }


    public FunctionSourceData getFunction(int functionId) {
        return functions.get(functionId);
    }

    public LocationParser getLocationParser() {
        return null;
    }
}

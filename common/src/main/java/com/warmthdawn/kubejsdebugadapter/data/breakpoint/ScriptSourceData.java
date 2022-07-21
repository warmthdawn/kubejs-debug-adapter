package com.warmthdawn.kubejsdebugadapter.data.breakpoint;

import com.warmthdawn.kubejsdebugadapter.utils.LocationParser;
import dev.latvian.mods.rhino.ast.FunctionNode;
import dev.latvian.mods.rhino.ast.ScriptNode;

import java.util.ArrayList;
import java.util.List;

public class ScriptSourceData {
    private String id;
    private String sourceString;
    private LocationParser parser;


    private final List<FunctionSourceData> functions = new ArrayList<>();

    public ScriptSourceData(String sourceId, String sourceString) {
        this.id = sourceId;
        this.sourceString = sourceString;
        this.parser = LocationParser.resolve(sourceId, sourceString);
    }

    public FunctionSourceData addFunction(ScriptNode scriptNode) {
        int id = functions.size();
        FunctionSourceData functionSourceData = new FunctionSourceData(scriptNode, id);
        functions.add(functionSourceData);
        return functionSourceData;
    }


    public FunctionSourceData getFunction(int functionId) {
        return functions.get(functionId);
    }

    public LocationParser getLocationParser() {
        return parser;
    }
}

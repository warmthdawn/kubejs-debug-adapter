package com.warmthdawn.kubejsdebugadapter.data.breakpoint;

import com.warmthdawn.kubejsdebugadapter.utils.BreakpointUtils;
import com.warmthdawn.kubejsdebugadapter.utils.LocationParser;
import dev.latvian.mods.rhino.Kit;
import dev.latvian.mods.rhino.ast.ScriptNode;
import it.unimi.dsi.fastutil.ints.IntIntPair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScriptSourceData {
    private String id;
    private String sourceString;
    private LocationParser parser;

    private boolean finished = false;
    private List<ScriptBreakpointInfo> locationList = null;

    private List<ScriptBreakpointInfo> majorLocationList = null;

    private final List<FunctionSourceData> functions = new ArrayList<>();

    public ScriptSourceData(String sourceId, String sourceString) {
        this.id = sourceId;
        this.sourceString = sourceString;
        this.parser = LocationParser.resolve(sourceId, sourceString);
    }

    public FunctionSourceData addFunction(ScriptNode scriptNode) {
        if (finished) {
            throw Kit.codeBug("Cannot add functions after finished compile");
        }
        int id = functions.size();
        FunctionSourceData functionSourceData = new FunctionSourceData(scriptNode, id);
        functions.add(functionSourceData);
        return functionSourceData;
    }

    public List<FunctionSourceData> getFunctions() {
        return functions;
    }

    public FunctionSourceData getFunction(int functionId) {
        return functions.get(functionId);
    }

    public LocationParser getLocationParser() {
        return parser;
    }

    public void finishCompile() {
        this.locationList = BreakpointUtils.collectBreakpoints(this);
        this.majorLocationList = BreakpointUtils.collectMajorBreakpoints(locationList);
        this.finished = true;
    }

    public String getId() {
        return id;
    }

    public boolean isFinished() {
        return finished;
    }

    public List<ScriptBreakpointInfo> getLocationList() {
        return locationList;
    }
    public List<ScriptBreakpointInfo> getMajorLocationList() {
        return majorLocationList;
    }


    public List<ScriptBreakpointInfo> getLocationList(int lineStart, int lineEnd, int columnStart, int columnEnd) {

        IntIntPair range = BreakpointUtils.binarySearchLocationsBetween(locationList.size(), it -> locationList.get(it).getLocation(), lineStart, lineEnd, columnStart, columnEnd);

        if (!(range.firstInt() < range.secondInt())) {
            return Collections.emptyList();
        }
        return locationList.subList(range.firstInt(), range.secondInt());
    }
}

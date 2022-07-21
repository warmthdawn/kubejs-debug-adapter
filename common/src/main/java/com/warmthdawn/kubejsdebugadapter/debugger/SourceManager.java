package com.warmthdawn.kubejsdebugadapter.debugger;

import com.warmthdawn.kubejsdebugadapter.data.breakpoint.ScriptSourceData;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SourceManager {
    private final Map<String, ScriptSourceData> compiledSources = new HashMap<>();

    private final Map<String, Boolean> loadedSources = new HashMap<>();


    public ScriptSourceData getSourceData(String sourceId) {
        return compiledSources.get(sourceId);
    }

    public boolean isSourceLoaded(String sourceId) {
        Boolean loaded = loadedSources.get(sourceId);
        return loaded != null && loaded;
    }

    public boolean hasCompiledSource(String sourceId) {
        return compiledSources.containsKey(sourceId);
    }


    public void setSourceLoaded(String sourceId, boolean loaded) {
        loadedSources.put(sourceId, loaded);
    }

    public void addSource(String sourceId, String sourceString) {
        compiledSources.put(sourceId, new ScriptSourceData(sourceId, sourceString));
    }





}

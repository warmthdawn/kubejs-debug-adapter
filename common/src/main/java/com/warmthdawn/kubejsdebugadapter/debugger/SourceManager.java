package com.warmthdawn.kubejsdebugadapter.debugger;

import com.warmthdawn.kubejsdebugadapter.data.breakpoint.ScriptSourceData;
import com.warmthdawn.kubejsdebugadapter.utils.BreakpointUtils;
import com.warmthdawn.kubejsdebugadapter.utils.PathUtil;
import dev.latvian.mods.rhino.ContextFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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

    public ScriptSourceData compileSource(String sourceId) {
        Path path = PathUtil.getSourcePath(sourceId);
        if(path == null || !Files.exists(path)) {
            return null;
        }
        try {
            String sourceString = Files.readString(path, StandardCharsets.UTF_8);
            return compileSource(sourceId, sourceString);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ScriptSourceData compileSource(String sourceId, String sourceString) {
        if (compiledSources.containsKey(sourceId)) {
            return compiledSources.get(sourceId);
        }

        ScriptSourceData data = new ScriptSourceData(sourceId, sourceString);
        compiledSources.put(sourceId, data);

        new ContextFactory().call(cx -> {
            return cx.compileString(sourceString, sourceId, 1, null);
        });

        return data;
    }


}

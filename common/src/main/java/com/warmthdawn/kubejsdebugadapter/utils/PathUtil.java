package com.warmthdawn.kubejsdebugadapter.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import dev.latvian.kubejs.KubeJSPaths;
import dev.latvian.kubejs.util.UtilsJS;
import org.eclipse.lsp4j.debug.Source;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class PathUtil {

    private static final BiMap<String, Path> scriptPackPaths = HashBiMap.create();

    static {
        scriptPackPaths.put("startup_scripts", KubeJSPaths.STARTUP_SCRIPTS);
        scriptPackPaths.put("server_scripts", KubeJSPaths.SERVER_SCRIPTS);
        scriptPackPaths.put("client_scripts", KubeJSPaths.CLIENT_SCRIPTS);
    }

    public static String getSourceId(Source source) {
        // TODO: Generated sources
        if (!source.getPath().endsWith(".js")) {
            return null;
        }

        Path file = Paths.get(source.getPath()).normalize().toAbsolutePath();
        for (Map.Entry<String, Path> entry : scriptPackPaths.entrySet()) {
            if (file.startsWith(entry.getValue())) {
                String namespace = entry.getKey();
                String fileName = entry.getValue().relativize(file).toString().replace(File.separatorChar, '/');
                return UtilsJS.getID(namespace + ":" + fileName);
            }
        }

        return null;

    }

    public static Source getDAPSource(String sourceId) {
        Path path = getSourcePath(sourceId);
        Source result = new Source();
        result.setPath(path.toString());
        result.setName(path.getFileName().toString());
        return result;
    }

    public static Path getSourcePath(String sourceId) {
        int i = sourceId.indexOf(":");
        if (i == -1) {
            throw new IllegalArgumentException();
        }

        String namespace = sourceId.substring(0, i);
        String path = sourceId.substring(i + 1);

        return scriptPackPaths.get(namespace).resolve(path);
    }
}

package com.warmthdawn.kubejsdebugadapter.fabric;

import com.warmthdawn.kubejsdebugadapter.KubeJSDebugAdapterPlatform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class KubeJSDebugAdapterPlatformImpl {
    /**
     * This is our actual method to {@link KubeJSDebugAdapterPlatform#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }


    public static void addLoadMessage(String message) {
//        StartupMessageManager.addModMessage(message);
    }
}

package com.warmthdawn.kubejsdebugadapter.forge;

import com.warmthdawn.kubejsdebugadapter.KubeJSDebugAdapterPlatform;
import net.minecraftforge.fml.StartupMessageManager;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class KubeJSDebugAdapterPlatformImpl {
    /**
     * This is our actual method to {@link KubeJSDebugAdapterPlatform#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static void addLoadMessage(String message) {
         StartupMessageManager.addModMessage(message);
    }
}

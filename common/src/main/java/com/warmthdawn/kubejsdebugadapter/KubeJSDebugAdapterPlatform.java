package com.warmthdawn.kubejsdebugadapter;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

public class KubeJSDebugAdapterPlatform {
    @ExpectPlatform
    public static Path getConfigDirectory() {
        // Just throw an error, the content should get replaced at runtime.
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void addLoadMessage(String message) {
        throw new AssertionError();
    }
}

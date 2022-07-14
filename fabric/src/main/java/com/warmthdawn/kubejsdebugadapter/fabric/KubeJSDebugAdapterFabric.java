package com.warmthdawn.kubejsdebugadapter.fabric;

import com.warmthdawn.kubejsdebugadapter.KubeJSDebugAdapter;
import net.fabricmc.api.ModInitializer;

public class KubeJSDebugAdapterFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        KubeJSDebugAdapter.init();
    }
}

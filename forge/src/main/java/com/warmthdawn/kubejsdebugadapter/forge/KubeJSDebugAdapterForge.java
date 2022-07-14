package com.warmthdawn.kubejsdebugadapter.forge;

import com.warmthdawn.kubejsdebugadapter.KubeJSDebugAdapter;
import me.shedaniel.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(KubeJSDebugAdapter.MOD_ID)
public class KubeJSDebugAdapterForge {
    public KubeJSDebugAdapterForge() {
        // Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(KubeJSDebugAdapter.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        KubeJSDebugAdapter.init();
    }
}

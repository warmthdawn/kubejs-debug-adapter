package com.warmthdawn.kubejsdebugadapter.fabric.asm;


import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;


public class EarlyRiser implements Runnable {
    @Override
    public void run() {
        MappingResolver remapper = FabricLoader.getInstance().getMappingResolver();


    }
}
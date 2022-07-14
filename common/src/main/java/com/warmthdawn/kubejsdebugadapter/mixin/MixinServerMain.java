package com.warmthdawn.kubejsdebugadapter.mixin;

import com.warmthdawn.kubejsdebugadapter.adapter.DebuggerLauncher;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public abstract class MixinServerMain {


    @Inject(method = "main", at = @At("HEAD"))
    private static void inject_main(String[] strings, CallbackInfo ci) {
        DebuggerLauncher.launch();
    }
}

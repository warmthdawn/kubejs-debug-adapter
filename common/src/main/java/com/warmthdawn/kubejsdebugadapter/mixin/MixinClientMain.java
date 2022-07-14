package com.warmthdawn.kubejsdebugadapter.mixin;

import com.warmthdawn.kubejsdebugadapter.KubeJSDebugAdapterPlatform;
import com.warmthdawn.kubejsdebugadapter.adapter.DebuggerLauncher;
import com.warmthdawn.kubejsdebugadapter.utils.NotifyDialog;
import net.minecraft.client.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(Main.class)
public abstract class MixinClientMain {


    @Inject(method = "main", at = @At("HEAD"))
    private static void inject_main(String[] strings, CallbackInfo ci) {
        System.setProperty("java.awt.headless", "false");
        int port = DebuggerLauncher.launch();
        NotifyDialog.showNotice(port);
    }
}

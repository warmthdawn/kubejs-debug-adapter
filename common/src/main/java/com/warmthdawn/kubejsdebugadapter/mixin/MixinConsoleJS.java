package com.warmthdawn.kubejsdebugadapter.mixin;

import com.warmthdawn.kubejsdebugadapter.adapter.DebuggerBridge;
import com.warmthdawn.kubejsdebugadapter.debugger.DebugContextListener;
import com.warmthdawn.kubejsdebugadapter.debugger.DebugRuntime;
import dev.latvian.kubejs.script.ScriptType;
import dev.latvian.kubejs.util.ConsoleJS;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(value = ConsoleJS.class, remap = false)
public abstract class MixinConsoleJS {


    @Shadow
    @Final
    private ScriptType type;

    @Inject(method = "writeToFile", at = @At(value = "HEAD"))
    private void inject_writeToFile(String type, String line, CallbackInfo ci) {
        DebuggerBridge bridge = DebugRuntime.getInstance().getBridge();
        if (bridge != null) {
            bridge.sendLogs(this.type.name, type, line);
        }
    }


}

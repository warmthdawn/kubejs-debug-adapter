package com.warmthdawn.kubejsdebugadapter.mixin;

import com.warmthdawn.kubejsdebugadapter.debugger.DebugContextListener;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScriptManager.class, remap = false)
public abstract class MixinScriptManager {

    private ContextFactory factory = null;
    private DebugContextListener listener;

    @Shadow
    @Final
    public ScriptType type;

    @Redirect(method = "load()V", at = @At(value = "INVOKE", target = "Ldev/latvian/mods/rhino/Context;enterWithNewFactory()Ldev/latvian/mods/rhino/Context;"))
    private Context inject_load() {
        factory = new ContextFactory();
        listener = new DebugContextListener(this.type.name);
        factory.addListener(listener);
        return factory.enterContext();
    }

    @Inject(method = "unload()V", at = @At("HEAD"))
    private void inject_unload(CallbackInfo ci) {
        if (factory != null) {
            factory.removeListener(listener);
            listener = null;
            factory = null;
        }
    }
}

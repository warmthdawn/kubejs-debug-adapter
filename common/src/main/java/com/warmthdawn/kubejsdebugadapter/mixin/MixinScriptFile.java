package com.warmthdawn.kubejsdebugadapter.mixin;

import com.warmthdawn.kubejsdebugadapter.debugger.DebugRuntime;
import com.warmthdawn.kubejsdebugadapter.debugger.SourceManager;
import dev.latvian.mods.kubejs.script.ScriptFile;
import dev.latvian.mods.kubejs.script.ScriptFileInfo;
import dev.latvian.mods.kubejs.script.ScriptSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.InputStream;

@Mixin(value = ScriptFile.class, remap = false)
public class MixinScriptFile {

    @Shadow
    @Final
    public ScriptSource source;

    @Shadow
    @Final
    public ScriptFileInfo info;

    @Inject(method = "load",
        locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Ldev/latvian/mods/rhino/Context;evaluateString(Ldev/latvian/mods/rhino/Scriptable;Ljava/lang/String;Ljava/lang/String;ILjava/lang/Object;)Ljava/lang/Object;"))
    private void inject_load(CallbackInfoReturnable<Boolean> cir, InputStream stream, String script) {
        SourceManager sourceManager = DebugRuntime.getInstance().getSourceManager();
        sourceManager.addSource(this.info.location, script);

    }
}

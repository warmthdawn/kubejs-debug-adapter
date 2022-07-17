package com.warmthdawn.kubejsdebugadapter.mixin;


import com.warmthdawn.kubejsdebugadapter.api.*;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.latvian.mods.rhino.Interpreter$CallFrame", remap = false)
public abstract class MixinInnerCallFrame implements IDebuggableCallFrame {


    @Mutable
    @Shadow
    @Final
    boolean useActivation;

    private DebugFrame debuggerFrame = null;

    @Override
    public void setDebuggerFrame(DebugFrame frame) {
        this.useActivation = true;
        this.debuggerFrame = frame;
    }


}

package com.warmthdawn.kubejsdebugadapter.mixin;

import com.warmthdawn.kubejsdebugadapter.api.IResultProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "dev.latvian.mods.rhino.Interpreter$ContinuationJump", remap = false)
public abstract class MixinInnerContinuationJump implements IResultProvider {

    @Shadow
    Object result;

    @Shadow
    double resultDbl;

    @Override
    public Object getResult() {
        return result;
    }

    @Override
    public double getResultDbl() {
        return resultDbl;
    }
}

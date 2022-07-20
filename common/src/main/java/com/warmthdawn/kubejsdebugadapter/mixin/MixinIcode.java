package com.warmthdawn.kubejsdebugadapter.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "dev.latvian.mods.rhino.Icode")
public class MixinIcode {

    @Shadow
    @Final
    @Mutable
    static int MIN_ICODE = -68;

}

package com.warmthdawn.kubejsdebugadapter.mixin;

import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "dev.latvian.mods.rhino.Icode", remap = false)
public abstract class MixinIcode {

    private static int MY_MIN_ICODE = -69;

    /**
     * @author WarmthDawn
     * @reason add_MIN_ICODE
     */
    @Overwrite
    static boolean validIcode(int icode) {
        return MY_MIN_ICODE <= icode && icode <= 0;
    }
}

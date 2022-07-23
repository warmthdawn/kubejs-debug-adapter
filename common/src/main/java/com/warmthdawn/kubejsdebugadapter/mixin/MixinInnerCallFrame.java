package com.warmthdawn.kubejsdebugadapter.mixin;


import com.warmthdawn.kubejsdebugadapter.api.*;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Interpreter;
import dev.latvian.mods.rhino.Scriptable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(targets = "dev.latvian.mods.rhino.Interpreter$CallFrame", remap = false)
public abstract class MixinInnerCallFrame implements IDebuggableCallFrame {


    @Mutable
    @Shadow
    @Final
    boolean useActivation;

    @Shadow
    @Final
    Scriptable thisObj;
    @Shadow
    Scriptable scope;
    @Shadow
    Object result;
    @Shadow
    double resultDbl;
    @Shadow
    int pc;
    private DebugFrame debuggerFrame = null;
    private static Field parentFrameField;


    @Override
    public void setDebuggerFrame(DebugFrame frame) {
        if(frame != null) {
            this.useActivation = true;
        }
        this.debuggerFrame = frame;
    }

    @Override
    public DebugFrame getDebuggerFrame() {
        return debuggerFrame;
    }

    @Override
    public Scriptable getScope() {
        return scope;
    }

    @Override
    public Scriptable getThisObj() {
        return thisObj;
    }

    @Override
    public IDebuggableCallFrame getParent() {
        try {
            if (parentFrameField == null) {
                parentFrameField = this.getClass().getDeclaredField("parentFrameField");
                parentFrameField.setAccessible(true);
            }

            return (IDebuggableCallFrame) parentFrameField.get(this);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Object getResult() {
        return result;
    }

    @Override
    public double getResultDbl() {
        return resultDbl;
    }

    @Override
    public void increasePC(int inc) {
        this.pc += inc;
    }

    @Override
    public int readBreakpointMeta(byte[] iCode) {

        return ((iCode[pc] & 0xFF) << 8) | (iCode[pc + 1] & 0xFF);
    }


}

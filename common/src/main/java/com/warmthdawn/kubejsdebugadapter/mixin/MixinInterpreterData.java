package com.warmthdawn.kubejsdebugadapter.mixin;

import com.warmthdawn.kubejsdebugadapter.api.DebuggableScript;
import com.warmthdawn.kubejsdebugadapter.api.IDebuggableScriptProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(targets = "dev.latvian.mods.rhino.InterpreterData", remap = false)
public abstract class MixinInterpreterData implements IDebuggableScriptProvider {

    @Shadow
    int itsFunctionType;
    @Shadow
    String itsName;
    @Shadow
    String[] argNames;
    @Shadow
    int argCount;
    @Shadow
    String itsSourceFile;
    @Shadow
    boolean topLevel;
    private DebuggableScript data;

    private int functionScriptId;


    @Override
    public void setFunctionScriptId(int functionScriptId) {
        this.functionScriptId = functionScriptId;
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void inject_init(CallbackInfo ci) {


        try {
            Field parentDataField = this.getClass().getDeclaredField("parentData");
            Field itsNestedFunctionsField = this.getClass().getDeclaredField("itsNestedFunctions");

            parentDataField.setAccessible(true);
            itsNestedFunctionsField.setAccessible(true);


            Object thisObj = this;

            data = new DebuggableScript() {

                private IDebuggableScriptProvider[] getNestedFunctions() {

                    try {
                        return (IDebuggableScriptProvider[]) itsNestedFunctionsField.get(thisObj);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return new IDebuggableScriptProvider[0];
                }

                private IDebuggableScriptProvider getItsParent() {
                    try {
                        return (IDebuggableScriptProvider) parentDataField.get(thisObj);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                public boolean isTopLevel() {
                    return topLevel;
                }

                @Override
                public boolean isFunction() {
                    return itsFunctionType == 0;
                }

                @Override
                public String getFunctionName() {
                    return itsName;
                }

                @Override
                public int getParamCount() {
                    return argCount;
                }

                @Override
                public int getParamAndVarCount() {
                    return argNames.length;
                }

                @Override
                public String getParamOrVarName(int index) {
                    return argNames[index];
                }

                @Override
                public String getSourceName() {
                    return itsSourceFile;
                }

                @Override
                public int getFunctionCount() {
                    IDebuggableScriptProvider[] itsNestedFunctions = getNestedFunctions();
                    return itsNestedFunctions == null ? 0 : itsNestedFunctions.length;
                }

                @Override
                public DebuggableScript getFunction(int index) {
                    IDebuggableScriptProvider[] itsNestedFunctions = getNestedFunctions();
                    return itsNestedFunctions[index].getDebuggableScript();
                }

                @Override
                public DebuggableScript getParent() {
                    IDebuggableScriptProvider itsParent = getItsParent();
                    if (itsParent == null) {
                        return null;
                    }
                    return itsParent.getDebuggableScript();
                }

                @Override
                public int getFunctionScriptId() {
                    return functionScriptId;
                }
            };
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }


    }


    @Override
    public DebuggableScript getDebuggableScript() {
        return data;
    }
}

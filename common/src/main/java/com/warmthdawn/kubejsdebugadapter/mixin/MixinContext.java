package com.warmthdawn.kubejsdebugadapter.mixin;

import com.warmthdawn.kubejsdebugadapter.api.DebuggableScript;
import com.warmthdawn.kubejsdebugadapter.api.Debugger;
import com.warmthdawn.kubejsdebugadapter.api.IDebuggableContext;
import com.warmthdawn.kubejsdebugadapter.api.IDebuggableScriptProvider;
import com.warmthdawn.kubejsdebugadapter.debugger.DebugRuntime;
import com.warmthdawn.kubejsdebugadapter.debugger.SourceManager;
import dev.latvian.mods.rhino.*;
import dev.latvian.mods.rhino.ast.AstRoot;
import dev.latvian.mods.rhino.ast.ScriptNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = Context.class, remap = false)
public abstract class MixinContext implements IDebuggableContext {

    @Shadow
    private boolean sealed;

    @Shadow
    static void onSealedMutation() {
    }

    private Debugger debugger;
    private Object debuggerData;


    @Override
    public final Debugger getDebugger() {
        return debugger;
    }

    @Override
    public final Object getDebuggerContextData() {
        return debuggerData;
    }

    @Override
    public final void setDebugger(Debugger debugger, Object contextData) {
        if (sealed) onSealedMutation();
        this.debugger = debugger;
        debuggerData = contextData;
    }


    @Inject(method = "evaluateString",
        at = @At(value = "INVOKE_ASSIGN",
            target = "Ldev/latvian/mods/rhino/Context;compileString(Ljava/lang/String;Ljava/lang/String;ILjava/lang/Object;)Ldev/latvian/mods/rhino/Script;"))
    private void inject_evaluateString(Scriptable scope, String source, String sourceName, int lineno, Object securityDomain, CallbackInfoReturnable<Object> cir) {

        if (sourceName == null) {
            return;
        }
        SourceManager sourceManager = DebugRuntime.getInstance().getSourceManager();
        if (!sourceManager.hasCompiledSource(sourceName)) {
            return;
        }
        sourceManager.setSourceLoaded(sourceName, true);
    }


}

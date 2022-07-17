package com.warmthdawn.kubejsdebugadapter.mixin;


import com.warmthdawn.kubejsdebugadapter.api.IDebuggableScriptProvider;
import dev.latvian.mods.rhino.CompilerEnvirons;
import dev.latvian.mods.rhino.Node;
import dev.latvian.mods.rhino.Token;
import dev.latvian.mods.rhino.ast.ScriptNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.latvian.mods.rhino.CodeGenerator", remap = false)
public abstract class MixinCodeGenerator {


    @Inject(method = "compile", at = @At("HEAD"))
    private void inject_compile(CompilerEnvirons compilerEnv, ScriptNode tree, boolean returnFunction, CallbackInfoReturnable<Object> cir) {


    }

    @Inject(method = "generateFunctionICode", at = @At("HEAD"))
    private void inject_generateFunctionICode(CallbackInfo ci) {


    }

    @Inject(method = "visitStatement", at = @At("HEAD"))
    private void inject_visitStatement(Node node, int initialStackDepth, CallbackInfo ci) {
        // TODO： 判断表达式类型


    }

    @Inject(method = "visitExpression", at = @At("HEAD"))
    private void inject_visitExpression(Node node, int contextFlags, CallbackInfo ci) {

        int type = node.getType();
        if(type == Token.CALL) {

        } else if(type == Token.NEW) {

        } else if(type == Token.REF_CALL) {

        }
    }
}

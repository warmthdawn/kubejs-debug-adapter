package com.warmthdawn.kubejsdebugadapter.mixin;


import com.warmthdawn.kubejsdebugadapter.KubeJSDebugAdapter;
import com.warmthdawn.kubejsdebugadapter.api.IDebuggableScriptProvider;
import com.warmthdawn.kubejsdebugadapter.data.ScriptDebuggerData;
import com.warmthdawn.kubejsdebugadapter.utils.AstUtils;
import com.warmthdawn.kubejsdebugadapter.utils.ExtendedConst;
import dev.latvian.mods.rhino.*;
import dev.latvian.mods.rhino.ast.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(targets = "dev.latvian.mods.rhino.CodeGenerator", remap = false)
public abstract class MixinCodeGenerator {


    @Shadow
    private ScriptNode scriptOrFn;
    private static Field itsDataField;

    private IDebuggableScriptProvider getData() {

        try {
            if (itsDataField == null) {
                itsDataField = this.getClass().getDeclaredField("itsData");
                itsDataField.setAccessible(true);
            }

            return (IDebuggableScriptProvider) itsDataField.get(this);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Inject(method = "compile", at = @At("HEAD"))
    private void inject_compile(CompilerEnvirons compilerEnv, ScriptNode tree, boolean returnFunction, CallbackInfoReturnable<Object> cir) {

        KubeJSDebugAdapter.log.info("-----------------------------------");
        KubeJSDebugAdapter.log.info(AstUtils.printNode(tree));
        KubeJSDebugAdapter.log.info("-----------------------------------");

    }

    @Inject(method = "generateFunctionICode", at = @At("HEAD"))
    private void inject_generateFunctionICode(CallbackInfo ci) {
        IDebuggableScriptProvider data = getData();
        if (data == null) {
            throw Kit.codeBug();
        }

        FunctionNode theFunction = (FunctionNode) scriptOrFn;

        int start = theFunction.getPosition();
        int end = theFunction.getLength() + start;

        ScriptDebuggerData debuggerData = new ScriptDebuggerData(start, end);
        data.setDebuggerData(debuggerData);
    }

    @Inject(method = "visitStatement", at = @At("HEAD"))
    private void inject_visitStatement(Node node, int initialStackDepth, CallbackInfo ci) {
        IDebuggableScriptProvider data = getData();
        if (data == null) {
            throw Kit.codeBug();
        }
        int type = node.getType();

        // 这几条语句需要特殊处理
        switch (type) {
            case Token.LABEL:
            case Token.LOOP:
            case Token.BLOCK:
            case Token.EMPTY:
            case Token.WITH:
            case Token.SCRIPT:
            case Token.LOCAL_BLOCK:
            case Token.FINALLY:
            case Token.TRY:
                return;
        }

        // 肯定是生成的表达式，不去管他
        if (node.getFirstChild() == null && node.getLineno() < 0) {
            return;
        }


        // TODO: 其实let和return差不多？
        if (type == Token.RETURN) {
            Node rv = node.getFirstChild();
            if (rv == null) {
                int position = node.getIntProp(ExtendedConst.RETURN_LOCATION_PROP, -1);
                int length = "return".length();
                if (position > 0) {
                    return;
                }
                return;
            }

        }

        if(node instanceof AstNode astNode) {
            int position = astNode.getAbsolutePosition();
            int length = astNode.getLength();

            if (position > 0) {
                // 可能的断点
                return;
            }
        }

        AstNode identifier = AstUtils.findFirstLiteralOrIdentifier(node);
        if (identifier != null) {
            int position = identifier.getAbsolutePosition();
            int length = identifier.getLength();

            if (position > 0) {
                // 可能的断点
                return;
            }
        }


        String nodeStr = AstUtils.printNode(node);

    }

    @Inject(method = "visitExpression", at = @At("HEAD"))
    private void inject_visitExpression(Node node, int contextFlags, CallbackInfo ci) {

        int type = node.getType();

        if (type == Token.CALL || type == Token.REF_CALL || type == Token.NEW) {
            IDebuggableScriptProvider data = getData();
            if (data == null) {
                throw Kit.codeBug();
            }
            Node child = node.getFirstChild();
            // Get Child's Name

            Name nameNode = AstUtils.findMethodName(child);

            if (nameNode != null && nameNode.getAbsolutePosition() > 0) {
                System.out.println(nameNode.getAbsolutePosition());
            } else {
                int rc = child.getIntProp(ExtendedConst.RC_LOCATION_PROP, -1);
                System.out.println(rc);
            }


        }
    }
}

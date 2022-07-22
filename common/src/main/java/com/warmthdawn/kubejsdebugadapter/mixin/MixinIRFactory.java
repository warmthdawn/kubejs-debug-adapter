package com.warmthdawn.kubejsdebugadapter.mixin;

import com.warmthdawn.kubejsdebugadapter.KubeJSDebugAdapter;
import com.warmthdawn.kubejsdebugadapter.utils.ExtendedConst;
import dev.latvian.mods.rhino.IRFactory;
import dev.latvian.mods.rhino.Node;
import dev.latvian.mods.rhino.Parser;
import dev.latvian.mods.rhino.Token;
import dev.latvian.mods.rhino.ast.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.warmthdawn.kubejsdebugadapter.utils.AstUtils.savePosition;

@Mixin(value = IRFactory.class, remap = false)
public abstract class MixinIRFactory extends Parser {


    // 基本的


    @Inject(method = "transform", at = @At("HEAD"))
    private void inject_transform(AstNode node, CallbackInfoReturnable<Node> cir) {
        switch (node.getType()) {
            case Token.BREAK, Token.CONTINUE, Token.NAME, Token.TRUE, Token.FALSE, Token.THIS, Token.NULL, Token.NUMBER -> {
                savePosition(node, node);
            }
        }
    }


    @Inject(method = "transformFunctionCall", at = @At("RETURN"))
    private void inject_transformFunctionCall(FunctionCall node, CallbackInfoReturnable<Node> cir) {
        Node returnValue = cir.getReturnValue();
        // 把方法调用表达式左括号的位置记下来
        returnValue.putIntProp(ExtendedConst.TOKEN_SPECIAL_POSITION_PROP, node.getLp() + node.getAbsolutePosition());
    }


    // 方法
    @Inject(method = "initFunction", at = @At("RETURN"))
    private static void inject_initFunction(FunctionNode fnNode, int functionIndex, Node statements, int functionType, CallbackInfoReturnable<Node> cir) {
        Name functionName = fnNode.getFunctionName();
        // 匿名方法
        if (functionName == null) {
            return;
        }
        Node returnValue = cir.getReturnValue();
        savePosition(returnValue, functionName);
    }


    // 在转换表达式的时候，保存名字标签的位置

    private final ThreadLocal<Name> _propertyNameNode = new ThreadLocal<>();

    @Inject(method = "transformPropertyGet", at = @At("HEAD"))
    private void inject_transformPropertyGet_HEAD(PropertyGet node, CallbackInfoReturnable<Node> cir) {
        _propertyNameNode.set(node.getProperty());
    }


    @Inject(method = "transformPropertyGet", at = @At("RETURN"))
    private void inject_transformPropertyGet_RETURN(PropertyGet node, CallbackInfoReturnable<Node> cir) {
        _propertyNameNode.set(null);
    }

    @Redirect(method = "createPropertyGet",
        at = @At(value = "INVOKE", target = "Ldev/latvian/mods/rhino/IRFactory;createName(Ljava/lang/String;)Ldev/latvian/mods/rhino/Node;"))
    private Node inject_createPropertyGet_createName(IRFactory instance, String s) {
        // 保存名称
        Node node = createName(s);
        savePosition(node, _propertyNameNode.get());
        return node;
    }


    // PropertyGet （如obj.foo）在获取的时候会重新创建一个Name节点从而丢失源码坐标信息，这里把他留下
    @Redirect(method = "createPropertyGet",
        at = @At(value = "INVOKE", target = "Ldev/latvian/mods/rhino/Node;newString(Ljava/lang/String;)Ldev/latvian/mods/rhino/Node;"))
    private Node inject_createPropertyGet_newString(String str) {
        // 保存名称
        Node node = Node.newString(str);
        savePosition(node, _propertyNameNode.get());
        return node;
    }


    @Inject(method = "transformReturn", at = @At("RETURN"))
    private void inject_transformReturn(ReturnStatement node, CallbackInfoReturnable<Node> cir) {
        Node returnValue = cir.getReturnValue();
        // 标记一下return的位置。
        returnValue.putIntProp(ExtendedConst.TOKEN_SPECIAL_POSITION_PROP, node.getAbsolutePosition());
    }


    @Inject(method = "transformYield", at = @At("RETURN"))
    private void inject_transformYield(Yield node, CallbackInfoReturnable<Node> cir) {
        Node returnValue = cir.getReturnValue();
        // 标记一下yield的位置。
        returnValue.putIntProp(ExtendedConst.TOKEN_SPECIAL_POSITION_PROP, node.getAbsolutePosition());
    }


    @Inject(method = "transformObjectLiteral", at = @At("RETURN"))
    private void inject_transformObjectLiteral(ObjectLiteral node, CallbackInfoReturnable<Node> cir) {
        Node returnValue = cir.getReturnValue();
        // 标记一下ObjectLiteral大括号的位置
        returnValue.putIntProp(ExtendedConst.TOKEN_SPECIAL_POSITION_PROP, node.getAbsolutePosition());
    }

    @Inject(method = "transformArrayLiteral", at = @At("RETURN"))
    private void inject_transformArrayLiteral(ArrayLiteral node, CallbackInfoReturnable<Node> cir) {
        Node returnValue = cir.getReturnValue();
        // 标记一下ArrayLiteral中括号的位置
        returnValue.putIntProp(ExtendedConst.TOKEN_SPECIAL_POSITION_PROP, node.getAbsolutePosition());
    }

    @Inject(method = "transformString", at = @At("RETURN"))
    private void inject_transformString(StringLiteral node, CallbackInfoReturnable<Node> cir) {
        Node returnValue = cir.getReturnValue();
        savePosition(returnValue, node);
    }

    @Inject(method = "transformTemplateLiteral", at = @At("RETURN"))
    private void inject_transformTemplateLiteral(TemplateLiteral node, CallbackInfoReturnable<Node> cir) {
        Node returnValue = cir.getReturnValue();
        savePosition(returnValue, node);
    }



}

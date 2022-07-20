package com.warmthdawn.kubejsdebugadapter.mixin;

import com.warmthdawn.kubejsdebugadapter.KubeJSDebugAdapter;
import com.warmthdawn.kubejsdebugadapter.utils.ExtendedConst;
import dev.latvian.mods.rhino.IRFactory;
import dev.latvian.mods.rhino.Node;
import dev.latvian.mods.rhino.Parser;
import dev.latvian.mods.rhino.ast.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = IRFactory.class, remap = false)
public abstract class MixinIRFactory extends Parser {

    @Shadow
    public abstract Node transform(AstNode node);

    @Inject(method = "transformFunctionCall", at = @At("RETURN"))
    private void inject_transformFunctionCall(FunctionCall node, CallbackInfoReturnable<Node> cir) {
        Node returnValue = cir.getReturnValue();
        // 把方法调用表达式左括号的位置记下来
        returnValue.putIntProp(ExtendedConst.RC_LOCATION_PROP, node.getLp() + node.getAbsolutePosition());
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
        copyPosition(node, _propertyNameNode.get());
        return node;
    }


    // PropertyGet （如obj.foo）在获取的时候会重新创建一个Name节点从而丢失源码坐标信息，这里把他留下
    @Redirect(method = "createPropertyGet",
        at = @At(value = "INVOKE", target = "Ldev/latvian/mods/rhino/Node;newString(Ljava/lang/String;)Ldev/latvian/mods/rhino/Node;"))
    private Node inject_createPropertyGet_newString(String str) {
        // 保存名称
        Node node = Node.newString(str);
        copyPosition(node, _propertyNameNode.get());
        return node;
    }


    @Inject(method = "transformReturn", at = @At("RETURN"))
    private void inject_transformReturn(ReturnStatement node, CallbackInfoReturnable<Node> cir) {
        Node returnValue = cir.getReturnValue();
        // 标记一下return的位置。
        returnValue.putIntProp(ExtendedConst.TOKEN_LOCATION_PROP, node.getAbsolutePosition());
    }


    @Inject(method = "transformYield", at = @At("RETURN"))
    private void inject_transformYield(Yield node, CallbackInfoReturnable<Node> cir) {
        Node returnValue = cir.getReturnValue();
        // 标记一下return的位置。
        returnValue.putIntProp(ExtendedConst.TOKEN_LOCATION_PROP, node.getAbsolutePosition());
    }


    @Inject(method = "transformObjectLiteral", at = @At("RETURN"))
    private void inject_transformObjectLiteral(ObjectLiteral node, CallbackInfoReturnable<Node> cir) {
        Node returnValue = cir.getReturnValue();
        // 标记一下ObjectLiteral大括号的位置
        returnValue.putIntProp(ExtendedConst.RC_LOCATION_PROP, node.getAbsolutePosition());
    }
    @Inject(method = "transformArrayLiteral", at = @At("RETURN"))
    private void inject_transformArrayLiteral(ArrayLiteral node, CallbackInfoReturnable<Node> cir) {
        Node returnValue = cir.getReturnValue();
        // 标记一下ObjectLiteral大括号的位置
        returnValue.putIntProp(ExtendedConst.RP_LOCATION_PROP, node.getAbsolutePosition());
    }
    @Inject(method = "transformString", at = @At("RETURN"))
    private void inject_transformString(StringLiteral node, CallbackInfoReturnable<Node> cir) {
        Node returnValue = cir.getReturnValue();
        copyPosition(returnValue, node);
    }
    @Inject(method = "transformTemplateLiteral", at = @At("RETURN"))
    private void inject_transformTemplateLiteral(TemplateLiteral node, CallbackInfoReturnable<Node> cir) {
        Node returnValue = cir.getReturnValue();
        copyPosition(returnValue, node);
    }

    private static void copyPosition(Node node, AstNode old) {
        if (old == null) {
            return;
        }
        if (node instanceof AstNode astNode) {
            astNode.setPosition(old.getAbsolutePosition());
            astNode.setLength(old.getLength());
            AstNode parent = astNode.getParent();
            if (parent != null) {
                astNode.setRelative(-parent.getAbsolutePosition());
            }
        } else {
            KubeJSDebugAdapter.log.warn("Failed to copy position info: node {} is not instance of AstNode", node);
        }
    }


}

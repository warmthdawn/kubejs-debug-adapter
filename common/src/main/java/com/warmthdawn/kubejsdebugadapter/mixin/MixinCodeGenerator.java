package com.warmthdawn.kubejsdebugadapter.mixin;


import com.warmthdawn.kubejsdebugadapter.KubeJSDebugAdapter;
import com.warmthdawn.kubejsdebugadapter.api.IDebuggableScriptProvider;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.BreakpointMeta;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.FunctionSourceData;
import com.warmthdawn.kubejsdebugadapter.data.breakpoint.ScriptSourceData;
import com.warmthdawn.kubejsdebugadapter.debugger.DebugRuntime;
import com.warmthdawn.kubejsdebugadapter.utils.AstUtils;
import com.warmthdawn.kubejsdebugadapter.utils.ExtendedConst;
import dev.latvian.mods.rhino.*;
import dev.latvian.mods.rhino.ast.*;
import it.unimi.dsi.fastutil.ints.IntStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.LinkedList;

@Mixin(targets = "dev.latvian.mods.rhino.CodeGenerator", remap = false)
public abstract class MixinCodeGenerator {


    @Shadow
    private ScriptNode scriptOrFn;

    @Shadow
    protected abstract void addIcode(int icode);

    @Shadow
    protected abstract void addUint16(int value);
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
    private FunctionSourceData functionSourceData;

    private void addStatementBreakpointMeta(int position, int length, boolean mustBreak) {
        if (length < 0) {
            length = 0;
        }
        BreakpointMeta breakpointMeta = functionSourceData.addStatementBreakpointMeta(position, length, mustBreak);
        statementMetaStack.pop();
        statementMetaStack.push(breakpointMeta.getId());
        addIcode(ExtendedConst.Icode_STATEMENT_BREAK);
        addUint16(breakpointMeta.getId());

    }

    private void addExpressionBreakpointMeta(int position, int length) {
        if (length < 0) {
            length = 0;
        }
        BreakpointMeta breakpointMeta = functionSourceData.addExpressionBreakpointMeta(position, length);

        Integer statementId = statementMetaStack.peek();
        if (statementId == null) {
            throw Kit.codeBug();
        }
        if (statementId >= 0) {
            functionSourceData.getStatementBreakpointMeta(statementId).addChild(breakpointMeta);
        }
        addIcode(ExtendedConst.Icode_EXPRESSION_BREAK);
        addUint16(breakpointMeta.getId());

    }

    private boolean hasSourceFile = false;


    @Inject(method = "generateFunctionICode", at = @At("HEAD"))
    private void inject_generateFunctionICode(CallbackInfo ci) {

        String sourceName = this.scriptOrFn.getSourceName();
        if (sourceName == null) {
            return;
        }
        hasSourceFile = true;

        if (this.functionSourceData != null) {
            throw Kit.codeBug("Called generateFunctionICode twice");
        }
        ScriptSourceData sourceData = DebugRuntime.getInstance().getSourceManager().getSourceData(this.scriptOrFn.getSourceName());

        FunctionNode theFunction = (FunctionNode) scriptOrFn;
        this.functionSourceData = sourceData.addFunction(theFunction);
        getData().setFunctionScriptId(functionSourceData.getId());

    }

    private final ArrayDeque<Integer> statementMetaStack = new ArrayDeque<>();

    @Inject(method = "visitStatement", at = @At("RETURN"))
    private void inject_visitStatement_RETURN(Node node, int initialStackDepth, CallbackInfo ci) {
        if (!hasSourceFile) {
            return;
        }
        statementMetaStack.pop();
    }

    @Inject(method = "visitStatement", at = @At("HEAD"))
    private void inject_visitStatement_HEAD(Node node, int initialStackDepth, CallbackInfo ci) {
        if (!hasSourceFile) {
            return;
        }
        statementMetaStack.push(-1);

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


        if (type == Token.RETURN || type == Token.YIELD || type == Token.YIELD_STAR) {
            int position = node.getIntProp(ExtendedConst.TOKEN_LOCATION_PROP, -1);
            int length = type == Token.RETURN ? "return".length() : "yield".length();
            if (position > 0) {
                this.addStatementBreakpointMeta(position, length, true);
                return;
            }
        }

        if (node instanceof AstNode astNode) {
            int position = astNode.getAbsolutePosition();
            int length = astNode.getLength();

            if (position > 0) {
                this.addStatementBreakpointMeta(position, length, false);
                return;
            }
        }

        Node locationalNode = AstUtils.findFirstLocationalNode(node);
        if (locationalNode != null) {
            int nodeType = locationalNode.getType();
            if (nodeType == Token.ARRAYLIT) {
                int position = node.getIntProp(ExtendedConst.RP_LOCATION_PROP, -1);
                if (position > 0) {
                    this.addStatementBreakpointMeta(position, 1, true);
                    return;
                }

            } else if (nodeType == Token.OBJECTLIT) {
                int position = node.getIntProp(ExtendedConst.RC_LOCATION_PROP, -1);
                if (position > 0) {
                    this.addStatementBreakpointMeta(position, 1, true);
                    return;
                }
            } else if (locationalNode instanceof AstNode astNode) {
                int position = astNode.getAbsolutePosition();
                int length = astNode.getLength();

                if (position > 0) {
                    this.addStatementBreakpointMeta(position, length, false);
                    return;
                }
            }
        }

        String typeStr = Token.typeToName(type);
        String nodeStr = AstUtils.printNode(node);


        KubeJSDebugAdapter.log.warn("Could not find breakpoint location for {} \n {}", typeStr, nodeStr);
    }

    @Inject(method = "visitExpression", at = @At("HEAD"))
    private void inject_visitExpression(Node node, int contextFlags, CallbackInfo ci) {
        if (!hasSourceFile) {
            return;
        }

        int type = node.getType();

        if (type == Token.CALL || type == Token.REF_CALL || type == Token.NEW) {
            Node child = node.getFirstChild();
            // Get Child's Name

            Name nameNode = AstUtils.findMethodName(child);

            if (nameNode != null && nameNode.getAbsolutePosition() > 0) {
                this.addExpressionBreakpointMeta(nameNode.getAbsolutePosition(), nameNode.getLength());

            } else {
                int rc = child.getIntProp(ExtendedConst.RC_LOCATION_PROP, -1);
                if (rc > 0) {
                    this.addExpressionBreakpointMeta(rc, 1);
                }
            }


        }
    }
}

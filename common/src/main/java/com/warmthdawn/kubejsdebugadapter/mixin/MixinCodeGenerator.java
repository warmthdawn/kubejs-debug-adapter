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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.ArrayDeque;

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
        addUint16(breakpointMeta.getId() & 0xFFFF);

    }

    private void addExpressionBreakpointMeta(int position, int length, boolean lowPriority) {
        if (length < 0) {
            length = 0;
        }

        BreakpointMeta breakpointMeta = functionSourceData.addExpressionBreakpointMeta(position, length, lowPriority);

        Integer statementId = statementMetaStack.peek();
        if (statementId == null) {
            throw Kit.codeBug();
        }
        if (statementId >= 0) {
            functionSourceData.getStatementBreakpointMeta(statementId).addChild(breakpointMeta);
        }
        addIcode(ExtendedConst.Icode_EXPRESSION_BREAK);
        addUint16(breakpointMeta.getId() & 0xFFFF);

    }

    private boolean hasSourceFile = false;


    @Inject(method = "compile", at = @At("RETURN"))
    private void inject_compile(CompilerEnvirons compilerEnv, ScriptNode tree, boolean returnFunction, CallbackInfoReturnable<Object> cir) {

        String sourceName = this.scriptOrFn.getSourceName();
        if (sourceName == null) {
            return;
        }
        ScriptSourceData sourceData = DebugRuntime.getInstance().getSourceManager().getSourceData(sourceName);
        sourceData.finishCompile();
    }

    @Inject(method = "generateICodeFromTree", at = @At("HEAD"))
    private void inject_generateICodeFromTree(CallbackInfo ci) {

        String sourceName = this.scriptOrFn.getSourceName();
        if (sourceName == null) {
            return;
        }
        hasSourceFile = true;

        if (this.functionSourceData != null) {
            throw Kit.codeBug("Called generateICodeFromTree twice");
        }
        ScriptSourceData sourceData = DebugRuntime.getInstance().getSourceManager().getSourceData(this.scriptOrFn.getSourceName());

        this.functionSourceData = sourceData.addFunction(scriptOrFn);
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

    private static final AstUtils.NodeTypeTree destructuringTree = AstUtils.typeTree(Token.EXPR_VOID,
        AstUtils.typeTree(Token.WITHEXPR,

            AstUtils.typeTree(
                Token.ENTERWITH,
                AstUtils.typeTree(Token.OBJECTLIT)
            ),


            AstUtils.typeTree(
                Token.WITH,
                AstUtils.typeTree(Token.COMMA)
            ),

            AstUtils.typeTree(Token.LEAVEWITH)

        )
    );


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

        {
            int position = node.getIntProp(ExtendedConst.TOKEN_POSITION_PROP, -1);

            if (position > 0) {
                int length = node.getIntProp(ExtendedConst.TOKEN_LENGTH_PROP, 1);
                this.addStatementBreakpointMeta(position, length, false);
                return;
            }
        }

        // 肯定是生成的表达式，不去管他
        if (node.getFirstChild() == null && node.getLineno() < 0) {
            return;
        }

        if (type == Token.EXPR_VOID) {
            Node firstChild = node.getFirstChild();
            if (firstChild != null) {
                // let const 和 var
                int firstChildType = firstChild.getType();
                if (firstChildType == Token.SETNAME || firstChildType == Token.SETCONST) {
                    if (firstChild.getFirstChild() != null) {
                        if (findSimpleStatement(firstChild.getFirstChild().getNext())) return;
                    }
                }
            }

            // 解构赋值
            if (AstUtils.hasTreeOf(node, destructuringTree.children)) {
                Node target = node.getFirstChild().getFirstChild().getFirstChild().getFirstChild();
                if (findSimpleStatement(target)) return;
            }


        }


        if (type == Token.RETURN || type == Token.YIELD || type == Token.YIELD_STAR) {
            int position = node.getIntProp(ExtendedConst.TOKEN_SPECIAL_POSITION_PROP, -1);
            int length = type == Token.RETURN ? "return".length() : "yield".length();
            if (position > 0) {
                this.addStatementBreakpointMeta(position, length, true);
                return;
            }
        }


        if (findSimpleStatement(node)) return;

        String typeStr = Token.typeToName(type);
        String nodeStr = AstUtils.printNode(node);


        KubeJSDebugAdapter.log.warn("Could not find breakpoint location for {} \n {}", typeStr, nodeStr);
    }

    private boolean findSimpleStatement(Node node) {
        Node locationalNode = AstUtils.findFirstLocationalNode(node);
        if (locationalNode != null) {
            int nodeType = locationalNode.getType();
            if (nodeType == Token.ARRAYLIT) {
                int position = node.getIntProp(ExtendedConst.TOKEN_SPECIAL_POSITION_PROP, -1);
                if (position > 0) {
                    this.addStatementBreakpointMeta(position, 1, true);
                    return true;
                }

            } else if (nodeType == Token.OBJECTLIT) {
                int position = node.getIntProp(ExtendedConst.TOKEN_SPECIAL_POSITION_PROP, -1);
                if (position > 0) {
                    this.addStatementBreakpointMeta(position, 1, true);
                    return true;
                }
            } else {
                int position = locationalNode.getIntProp(ExtendedConst.TOKEN_POSITION_PROP, -1);
                int length = locationalNode.getIntProp(ExtendedConst.TOKEN_LENGTH_PROP, 1);

                if (position > 0) {
                    this.addStatementBreakpointMeta(position, length, false);
                    return true;
                }
            }
        }
        return false;
    }


    @Inject(method = "visitExpression", at = @At(value = "HEAD"))
    private void inject_visitExpression_HEAD(Node node, int contextFlags, CallbackInfo ci) {
        if (!hasSourceFile) {
            return;
        }
        int type = node.getType();
        if (type == Token.SETCONST || type == Token.SETNAME) {
            Node nameNode = node.getFirstChild();
            if (nameNode == null) {
                return;
            }
            int destructuring = nameNode.getIntProp(ExtendedConst.DESTRUCTURING_SET_FLAG_PROP, -1);
            if (destructuring > 0) {
                int position = nameNode.getIntProp(ExtendedConst.TOKEN_POSITION_PROP, -1);
                if (position > 0) {
                    int length = nameNode.getIntProp(ExtendedConst.TOKEN_LENGTH_PROP, 1);
                    this.addExpressionBreakpointMeta(position, length, true);
                }
            }
        }
    }

    @Inject(method = "visitExpression", at = @At(value = "INVOKE",
        target = "Ldev/latvian/mods/rhino/Node;getIntProp(II)I"), allow = 1)
    private void inject_visitExpression_CallExp(Node node, int contextFlags, CallbackInfo ci) {
        if (!hasSourceFile) {
            return;
        }
        int type = node.getType();
        if (type != Token.CALL && type != Token.REF_CALL && type != Token.NEW) {
            throw Kit.codeBug();
        }
        Node child = node.getFirstChild();
        // Get Child's Name

        Name nameNode = AstUtils.findMethodName(child);

        if (nameNode != null) {
            int position = nameNode.getIntProp(ExtendedConst.TOKEN_POSITION_PROP, -1);
            int length = nameNode.getIntProp(ExtendedConst.TOKEN_LENGTH_PROP, 1);
            if (position > 0) {
                this.addExpressionBreakpointMeta(position, length, false);
            }

        } else {
            int rc = node.getIntProp(ExtendedConst.TOKEN_SPECIAL_POSITION_PROP, -1);
            if (rc > 0) {
                this.addExpressionBreakpointMeta(rc, 1, false);
            }
        }
    }
}

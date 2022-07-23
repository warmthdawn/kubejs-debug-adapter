package com.warmthdawn.kubejsdebugadapter.mixin;


import com.warmthdawn.kubejsdebugadapter.utils.CompletionCompileEnv;
import com.warmthdawn.kubejsdebugadapter.utils.ExtendedConst;
import dev.latvian.mods.rhino.CompilerEnvirons;
import dev.latvian.mods.rhino.Node;
import dev.latvian.mods.rhino.Parser;
import dev.latvian.mods.rhino.ast.AstNode;
import dev.latvian.mods.rhino.ast.Name;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.warmthdawn.kubejsdebugadapter.utils.AstUtils.savePosition;

@Mixin(value = Parser.class, remap = false)
public abstract class MixinParser {


    @Shadow
    protected abstract Node createName(int type, String name, Node child);

    @Shadow
    private CompilerEnvirons compilerEnv;
    @Shadow
    private int syntaxErrorCount;
    private final ThreadLocal<Name> _nameNode = new ThreadLocal<>();

    @Redirect(method = "destructuringArray",
        at = @At(value = "INVOKE", target = "Ldev/latvian/mods/rhino/ast/AstNode;getString()Ljava/lang/String;"))
    private String inject_destructuringArray_getString(AstNode instance) {
        _nameNode.set((Name) instance);
        // 保存名称
        return instance.getString();
    }

    @Redirect(method = "destructuringArray",
        at = @At(value = "INVOKE", target = "Ldev/latvian/mods/rhino/Parser;createName(ILjava/lang/String;Ldev/latvian/mods/rhino/Node;)Ldev/latvian/mods/rhino/Node;"))
    private Node inject_destructuringArray_createName(Parser instance, int type, String name, Node child) {
        // 保存名称
        Node node = createName(type, name, child);
        savePosition(node, _nameNode.get());
        node.putIntProp(ExtendedConst.DESTRUCTURING_SET_FLAG_PROP, 1);
        _nameNode.set(null);
        return node;
    }

    @Redirect(method = "destructuringObject",
        at = @At(value = "INVOKE", target = "Ldev/latvian/mods/rhino/ast/Name;getIdentifier()Ljava/lang/String;"))
    private String inject_destructuringObject_getString(Name instance) {
        _nameNode.set(instance);
        // 保存名称
        return instance.getString();
    }

    @Redirect(method = "destructuringObject",
        at = @At(value = "INVOKE", target = "Ldev/latvian/mods/rhino/Parser;createName(ILjava/lang/String;Ldev/latvian/mods/rhino/Node;)Ldev/latvian/mods/rhino/Node;"))
    private Node inject_destructuringObject_createName(Parser instance, int type, String name, Node child) {
        // 保存名称
        Node node = createName(type, name, child);
        savePosition(node, _nameNode.get());
        node.putIntProp(ExtendedConst.DESTRUCTURING_SET_FLAG_PROP, 1);
        _nameNode.set(null);
        return node;
    }


    @Redirect(method = "parse()Ldev/latvian/mods/rhino/ast/AstRoot;",
        at = @At(value = "FIELD", target = "Ldev/latvian/mods/rhino/Parser;syntaxErrorCount:I", opcode = Opcodes.GETFIELD, ordinal = 0))
    private int inject_parse_error(Parser instance) {
        if (compilerEnv instanceof CompletionCompileEnv) {
            return 0;
        }
        return syntaxErrorCount;

    }


}

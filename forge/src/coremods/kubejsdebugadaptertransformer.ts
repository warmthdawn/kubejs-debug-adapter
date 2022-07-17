import {
    AbstractInsnNode,
    CoreMods,
    FieldInsnNode,
    InsnList,
    InsnNode, JumpInsnNode,
    LabelNode, MethodInsnNode,
    MethodNode,
    Opcodes,
    VarInsnNode
} from "./coremods";

function initializeCoreMods(): CoreMods {

    return {
        m_interpreter_interpretLoop: {
            target: {
                type: "METHOD",
                class: "dev.latvian.mods.rhino.Interpreter",
                methodName: "interpretLoop",
                methodDesc: "(Ldev/latvian/mods/rhino/Context;Ldev/latvian/mods/rhino/Interpreter$CallFrame;Ljava/lang/Object;)Ljava/lang/Object;"
            },
            transformer(method: MethodNode) {

                const arrayLength = method.instructions.size();


                let lastLabel = null;
                find:
                for (let i = 0; i < arrayLength; i++) {
                    const beginInsn = method.instructions.get(i)!!;

                    if (beginInsn.getType() === AbstractInsnNode.LABEL) {
                        lastLabel = (beginInsn as LabelNode).getLabel();
                    }

                    if (beginInsn.getType() !== AbstractInsnNode.FRAME) {
                        break
                    }
                    if(lastLabel === null) {
                        continue
                    }

                    // 外层循环的标签
                    let loopLabel = lastLabel!!;

                    // switch(op) 的位置
                    let switchIndex = -1;
                    // 局部变量'op'的位置
                    let opPos = -1
                    let injectPosition = null;


                    // 找到 int op = iCode[frame.pc++];
                    let hasGetFrame = false;
                    let hasGetPC = false;
                    let hasPutPC = false;
                    let verified = false;

                    // 简单确保一些这段代码包括下面几个指令
                    for (let j = i + 1; j < arrayLength; j++) {
                        const insn = method.instructions.get(j)!!;
                        if (insn.getOpcode() === Opcodes.ALOAD && (insn as VarInsnNode).var === 1) {
                            hasGetFrame = true;
                        }
                        if (hasGetFrame &&
                            insn.getOpcode() === Opcodes.GETFIELD &&
                            (insn as FieldInsnNode).name.indexOf("CallFrame.pc") > 0) {
                            hasGetPC = true;
                        }
                        if (hasGetPC &&
                            insn.getOpcode() === Opcodes.PUTFIELD &&
                            (insn as FieldInsnNode).name.indexOf("CallFrame.pc") > 0) {
                            hasPutPC = true;
                        }
                        if (hasPutPC && insn.getOpcode() === Opcodes.BALOAD) {
                            let nextInsn = method.instructions.get(i + 1)!!;
                            if (nextInsn.getOpcode() === Opcodes.ISTORE) {
                                injectPosition = nextInsn;
                                opPos = (nextInsn as VarInsnNode).var;
                                verified = true;
                                break;
                            }
                        }
                    }
                    if (!verified) {
                        continue;
                    }


                    // 找switch:

                    for (let j = i + 1; j < arrayLength; j++) {
                        const insn = method.instructions.get(j)!!;
                        if (insn.getOpcode() !== Opcodes.TABLESWITCH) {
                            continue;
                        }
                        const prevInsn = method.instructions.get(j - 1)!!;
                        //确保一下switch的参数正确
                        if (prevInsn.getOpcode() !== Opcodes.ILOAD ||
                            (prevInsn as VarInsnNode).var !== opPos) {
                            continue find;
                        }

                        switchIndex = i;
                        break;
                    }
                    if (switchIndex < 0) {
                        continue;
                    }


                    const toInject = new InsnList();

                    // 插入指令

                    /**
                     * boolean flag = processExtraOp(op, frame)
                     * if(flag) {
                     *     continue;
                     * }
                     */

                    /**
                     * =>
                     * ILOAD $opPos
                     * ALOAD 1
                     * INVOKESTATIC Lcom/warmthdawn/kubejsdebugadapter/asm/PatchInterpreter;processExtraOp(ILjava/lang/Object;)Z
                     * IFNE $loopLabel
                     *
                     */

                    toInject.insert(new VarInsnNode(Opcodes.ILOAD, opPos));
                    toInject.insert(new VarInsnNode(Opcodes.ALOAD, 1));
                    toInject.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/warmthdawn/kubejsdebugadapter/asm/PatchInterpreter", "processExtraOp", "(ILjava/lang/Object;)Z"));
                    toInject.insert(new JumpInsnNode(Opcodes.IFNE, new LabelNode(loopLabel)));



                    method.instructions.insert(injectPosition!!, toInject);



                }
                return method;
            }
        }
    }

}
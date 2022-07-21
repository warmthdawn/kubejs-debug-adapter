var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode')
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode')
var AbstractInsnNode = Java.type('org.objectweb.asm.tree.AbstractInsnNode')
var InsnList = Java.type('org.objectweb.asm.tree.InsnList')
var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode')
var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode')
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode')


function initializeCoreMod() {

    return {
        m_interpreter_interpretLoop: {
            target: {
                type: "METHOD",
                class: "dev.latvian.mods.rhino.Interpreter",
                methodName: "interpretLoop",
                methodDesc: "(Ldev/latvian/mods/rhino/Context;Ldev/latvian/mods/rhino/Interpreter$CallFrame;Ljava/lang/Object;)Ljava/lang/Object;"
            },
            transformer: function (method) {
                print("KubeJS Debug Adapter Transforming: Begin transform of " + method.name);

                var arrayLength = method.instructions.size();

                var succeed = false;

                var lastLabel = null;
                var i = 0
                for (; i < arrayLength; i++) {
                    var find = method.instructions.get(i);
                    if (find.getOpcode() === Opcodes.PUTFIELD &&
                        find.owner === "dev/latvian/mods/rhino/Context" &&
                        find.name === "lastInterpreterFrame") {
                        break;
                    }
                }
                i++;
                if(i >= arrayLength) {
                    print("KubeJS Debug Adapter Transforming: Could not find get lastInterpreterFrame");
                    return method;
                }
                find:
                    for (; i < arrayLength; i++) {
                        var beginInsn = method.instructions.get(i);

                        if (beginInsn.getType() === AbstractInsnNode.LABEL) {
                            lastLabel = beginInsn.getLabel();
                        }

                        if (beginInsn.getType() !== AbstractInsnNode.FRAME) {
                            continue
                        }
                        if (lastLabel === null) {
                            continue
                        }


                        // 外层循环的标签
                        var loopLabel = lastLabel;

                        // switch(op) 的位置
                        var switchIndex = -1;
                        // 局部变量'op'的位置
                        var opPos = -1
                        var injectPosition = null;


                        // 找到 int op = iCode[frame.pc++];
                        // 确保四条关键指令存在基本上久差不多了： 读icode, 读frame.pc，frame.pc，写 op
                        print("KubeJS Debug Adapter Transforming: Finding iCode[frame.pc++]...")
                        var iCodePos = -1;
                        var hasGetFrame = false;
                        var hasGetPC = false;
                        var hasPutPC = false;
                        var verified = false;

                        var j = i + 1;
                        var insn, prevInsn;
                        // 简单确保一些这段代码包括下面几个指令
                        for (; j < arrayLength; j++) {
                            insn = method.instructions.get(j);
                            if (insn.getOpcode() === Opcodes.ALOAD && insn.var === 1) {
                                prevInsn = method.instructions.get(j - 1);
                                if (prevInsn.getOpcode() === Opcodes.ALOAD) {
                                    iCodePos = prevInsn.var;
                                    hasGetFrame = true;
                                }
                            }
                            if (hasGetFrame &&
                                insn.getOpcode() === Opcodes.GETFIELD &&
                                insn.name === 'pc') {
                                hasGetPC = true;
                            }
                            if (hasGetPC &&
                                insn.getOpcode() === Opcodes.PUTFIELD &&
                                insn.name === 'pc') {
                                hasPutPC = true;
                            }
                            if (hasPutPC && insn.getOpcode() === Opcodes.BALOAD) {
                                var nextInsn = method.instructions.get(j + 1);
                                if (nextInsn.getOpcode() === Opcodes.ISTORE) {
                                    injectPosition = nextInsn;
                                    opPos = nextInsn.var;
                                    verified = true;
                                    break;
                                }
                            }
                        }
                        if (!verified) {
                            print("KubeJS Debug Adapter Transforming: Unmatch instruction sequence, search in remaining instructions...")
                            continue;
                        }

                        print("KubeJS Debug Adapter Transforming: Finding switch(pc)....")
                        // 找switch:

                        for (; j < arrayLength; j++) {
                            insn = method.instructions.get(j);
                            if (insn.getOpcode() !== Opcodes.TABLESWITCH) {
                                continue;
                            }
                            prevInsn = method.instructions.get(j - 1);
                            //确保一下switch的参数正确
                            if (prevInsn.getOpcode() !== Opcodes.ILOAD ||
                                prevInsn.var !== opPos) {
                                continue find;
                            }

                            switchIndex = j;
                            break;
                        }
                        if (switchIndex < 0) {
                            continue;
                        }


                        var toInject = new InsnList();

                        // 插入指令

                        /**
                         * boolean flag = processExtraOp(this, op, frame, iCode)
                         * if(flag) {
                         *     continue;
                         * }
                         */

                        /**
                         * =>
                         * ALOAD 0
                         * ILOAD $opPos
                         * ALOAD 1
                         * ALOAD $iCodePos
                         * INVOKESTATIC Lcom/warmthdawn/kubejsdebugadapter/asm/KDAPatches;processExtraOp(Ldev/latvian/mods/rhino/Context;ILjava/lang/Object;[B)Z
                         *
                         * IFNE $loopLabel
                         *
                         */

                        toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        toInject.add(new VarInsnNode(Opcodes.ILOAD, opPos));
                        toInject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        toInject.add(new VarInsnNode(Opcodes.ALOAD, iCodePos));
                        toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/warmthdawn/kubejsdebugadapter/asm/KDAPatches", "processExtraOp", "(Ldev/latvian/mods/rhino/Context;ILjava/lang/Object;[B)Z"));
                        toInject.add(new JumpInsnNode(Opcodes.IFNE, new LabelNode(loopLabel)));


                        print("KubeJS Debug Adapter Transforming: Applying transformation to " + method.name);
                        method.instructions.insert(injectPosition, toInject);
                        succeed = true;
                        print("KubeJS Debug Adapter Transforming: Successfully transformed " + method.name);

                        break;


                    }
                if (!succeed) {
                    print("KubeJS Debug Adapter Transforming: Failed to transform " + method.name);
                }
                return method;
            }
        },


        m_interpreter_enterFrame: {
            //Ldev/latvian/mods/rhino/Interpreter;enterFrame(Ldev/latvian/mods/rhino/Context;Ldev/latvian/mods/rhino/Interpreter$CallFrame;[Ljava/lang/Object;Z)V
            target: {
                type: "METHOD",
                class: "dev.latvian.mods.rhino.Interpreter",
                methodName: "enterFrame",
                methodDesc: "(Ldev/latvian/mods/rhino/Context;Ldev/latvian/mods/rhino/Interpreter$CallFrame;[Ljava/lang/Object;Z)V"
            },
            transformer: function (method) {
                print("KubeJS Debug Adapter Transforming: Begin transform of " + method.name);
                var succeed = false;
                var arrayLength = method.instructions.size();
                for (var i = arrayLength - 1; i >= 0; i--) {
                    var insn = method.instructions.get(i);
                    if (insn.getOpcode() !== Opcodes.RETURN) {
                        continue;
                    }

                    var toInject = new InsnList();
                    toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    toInject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    toInject.add(new VarInsnNode(Opcodes.ALOAD, 2));
                    toInject.add(new VarInsnNode(Opcodes.ILOAD, 3));

                    // Lcom/warmthdawn/kubejsdebugadapter/asm/KDAPatches;enterFrame(Ljava/lang/Object;Ldev/latvian/mods/rhino/Context;Ljava/lang/Object;)V
                    toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/warmthdawn/kubejsdebugadapter/asm/KDAPatches", "enterDebugFrame", "(Ldev/latvian/mods/rhino/Context;Ljava/lang/Object;[Ljava/lang/Object;Z)V"));

                    print("KubeJS Debug Adapter Transforming: Applying transformation to " + method.name);

                    method.instructions.insertBefore(insn, toInject);
                    print("KubeJS Debug Adapter Transforming: Successfully transformed " + method.name);
                    succeed = true;

                    break;
                }
                if (!succeed) {
                    print("KubeJS Debug Adapter Transforming: Failed to transform " + method.name);
                }

                return method;
            }
        },


        m_interpreter_exitFrame: {
            // Ldev/latvian/mods/rhino/Interpreter;exitFrame(Ldev/latvian/mods/rhino/Context;Ldev/latvian/mods/rhino/Interpreter$CallFrame;Ljava/lang/Object;)V

            target: {
                type: "METHOD",
                class: "dev.latvian.mods.rhino.Interpreter",
                methodName: "exitFrame",
                methodDesc: "(Ldev/latvian/mods/rhino/Context;Ldev/latvian/mods/rhino/Interpreter$CallFrame;Ljava/lang/Object;)V"
            },
            transformer: function (method) {
                print("KubeJS Debug Adapter Transforming: Begin transform of " + method.name);
                var succeed = false;
                var arrayLength = method.instructions.size();
                for (var i = arrayLength - 1; i >= 0; i--) {
                    var insn = method.instructions.get(i);
                    if (insn.getOpcode() !== Opcodes.RETURN) {
                        continue;
                    }

                    var toInject = new InsnList();
                    toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    toInject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    toInject.add(new VarInsnNode(Opcodes.ALOAD, 2));

                    // Lcom/warmthdawn/kubejsdebugadapter/asm/KDAPatches;exitDebugFrame(Ldev/latvian/mods/rhino/Context;Ljava/lang/Object;Ljava/lang/Object;)V
                    toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/warmthdawn/kubejsdebugadapter/asm/KDAPatches", "exitDebugFrame", "(Ldev/latvian/mods/rhino/Context;Ljava/lang/Object;Ljava/lang/Object;)V"));

                    print("KubeJS Debug Adapter Transforming: Applying transformation to " + method.name);

                    method.instructions.insertBefore(insn, toInject);
                    print("KubeJS Debug Adapter Transforming: Successfully transformed " + method.name);
                    succeed = true;

                    break;
                }
                if (!succeed) {
                    print("KubeJS Debug Adapter Transforming: Failed to transform " + method.name);
                }

                return method;
            }
        },


        m_callFrame_init: {
            target: {
                type: "METHOD",
                class: "dev.latvian.mods.rhino.Interpreter$CallFrame",
                methodName: "<init>",
                methodDesc: "(Ldev/latvian/mods/rhino/Context;Ldev/latvian/mods/rhino/Scriptable;Ldev/latvian/mods/rhino/InterpretedFunction;Ldev/latvian/mods/rhino/Interpreter$CallFrame;)V"
            },
            transformer: function (method) {
                print("KubeJS Debug Adapter Transforming: Begin transform of " + method.name);
                var succeed = false;
                var arrayLength = method.instructions.size();
                for (var i = 0; i < arrayLength; i++) {
                    var insn = method.instructions.get(i);
                    if (insn.getOpcode() !== Opcodes.INVOKESPECIAL) {
                        hasSuper = true
                        continue;
                    }

                    var toInject = new InsnList();
                    toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    toInject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    toInject.add(new VarInsnNode(Opcodes.ALOAD, 3));
                    toInject.add(new FieldInsnNode(Opcodes.GETFIELD, "dev/latvian/mods/rhino/InterpretedFunction", "idata", "Ldev/latvian/mods/rhino/InterpreterData;"));

                    // Lcom/warmthdawn/kubejsdebugadapter/asm/KDAPatches;initDebugFrame(Ljava/lang/Object;Ldev/latvian/mods/rhino/Context;Ljava/lang/Object;)V
                    toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/warmthdawn/kubejsdebugadapter/asm/KDAPatches", "initDebugFrame", "(Ljava/lang/Object;Ldev/latvian/mods/rhino/Context;Ljava/lang/Object;)V"));

                    print("KubeJS Debug Adapter Transforming: Applying transformation to " + method.name);

                    method.instructions.insert(insn, toInject);
                    print("KubeJS Debug Adapter Transforming: Successfully transformed " + method.name);
                    succeed = true;

                    break;
                }
                if (!succeed) {
                    print("KubeJS Debug Adapter Transforming: Failed to transform " + method.name);
                }

                return method;
            }
        },


        m_context_compileImpl: {
            //Ldev/latvian/mods/rhino/Context;compileImpl(Ldev/latvian/mods/rhino/Scriptable;Ljava/lang/String;Ljava/lang/String;ILjava/lang/Object;ZLdev/latvian/mods/rhino/Evaluator;Ldev/latvian/mods/rhino/ErrorReporter;)Ljava/lang/Object;
            target: {
                type: "METHOD",
                class: "dev.latvian.mods.rhino.Context",
                methodName: "compileImpl",
                methodDesc: "(Ldev/latvian/mods/rhino/Scriptable;Ljava/lang/String;Ljava/lang/String;ILjava/lang/Object;ZLdev/latvian/mods/rhino/Evaluator;Ldev/latvian/mods/rhino/ErrorReporter;)Ljava/lang/Object;"
            },
            transformer: function (method) {
                print("KubeJS Debug Adapter Transforming: Begin transform of " + method.name);
                var finished = 0;
                var arrayLength = method.instructions.size();
                for (var i = 0; i < arrayLength; i++) {
                    var insn = method.instructions.get(i);
                    if (!(insn.getOpcode() === Opcodes.INVOKEINTERFACE && insn.name === "compile" && insn.owner === "dev/latvian/mods/rhino/Evaluator")) {
                        continue;
                    }

                    var next = method.instructions.get(i + 1);
                    if (!next || next.getOpcode() !== Opcodes.ASTORE) {
                        print("KubeJS Debug Adapter Transforming: Could not find astore instruction after compile call");
                        continue;
                    }
                    var varId = next.var;


                    var toInject = new InsnList();
                    toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    toInject.add(new VarInsnNode(Opcodes.ALOAD, varId));
                    toInject.add(new VarInsnNode(Opcodes.ALOAD, 2));

                    // Lcom/warmthdawn/kubejsdebugadapter/asm/KDAPatches;debugScriptComplied(Ldev/latvian/mods/rhino/Context;Ljava/lang/Object;Ljava/lang/String;)V
                    toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/warmthdawn/kubejsdebugadapter/asm/KDAPatches", "debugScriptComplied", "(Ldev/latvian/mods/rhino/Context;Ljava/lang/Object;Ljava/lang/String;)V"));

                    print("KubeJS Debug Adapter Transforming: Applying transformation to " + method.name);
                    method.instructions.insert(next, toInject);
                    finished++;

                    if (finished >= 2) {
                        break;
                    }

                }
                if (finished < 2) {
                    print("KubeJS Debug Adapter Transforming: Failed to transform " + method.name);
                    throw new Error("Failed to transform " + method.name);
                } else {
                    print("KubeJS Debug Adapter Transforming: Successfully transformed " + method.name);
                }

                return method;
            }
        },

    }

}
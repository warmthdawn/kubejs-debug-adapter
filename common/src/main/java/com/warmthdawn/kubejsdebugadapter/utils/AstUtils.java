package com.warmthdawn.kubejsdebugadapter.utils;

import dev.latvian.mods.rhino.Node;
import dev.latvian.mods.rhino.Token;
import dev.latvian.mods.rhino.ast.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.Consumer;

public class AstUtils {
    private static final Logger log = LogManager.getLogger();

    public static String printNode(Node node) {
        StringBuilder sb = new StringBuilder();
        _printNode(sb, node, 0);
        return sb.toString();
    }


    private static void appendIndent(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
    }

    private static void _printNode(StringBuilder builder, Node node, int depth) {
        appendIndent(builder, depth);
//        builder.append(node.toString());

        builder.append(Token.typeToName(node.getType()));

        if (node instanceof Label || node instanceof Name || node instanceof StringLiteral || node instanceof NumberLiteral) {
            builder.append(" (").append(node).append(")");
        }

        if (node.getFirstChild() != null) {

            builder.append("{").append("\n");
            forEachChildren(node, child -> _printNode(builder, child, depth + 1));
            appendIndent(builder, depth);
            builder.append("}").append("\n");
        } else {
            builder.append("\n");
        }

    }


    public static Name findMethodName(Node node) {

        int type = node.getType();
        if (node instanceof Name) {
            return (Name) node;
        }
        if (type == Token.GETPROP) {
            Node id = node.getLastChild();
            if (id instanceof Name) {
                return (Name) id;
            } else {
                log.warn("Node：{} has token of GetProp but last child is not a Name", node);
            }
        }
        return null;
    }

    public static boolean isLiteralOrIdentifier(Node node) {
        int type = node.getType();
        return type == Token.STRING || type == Token.NUMBER || type == Token.NAME || type == Token.BINDNAME || type == Token.TRUE || type == Token.FALSE || type == Token.NULL;
    }

    public static String idToString(AstNode node) {
        if (node instanceof Name) {
            return node.getString();
        }
        return switch (node.getType()) {
            case Token.STRING, Token.NUMBER -> node.toString();
            case Token.TRUE -> "true";
            case Token.FALSE -> "false";
            case Token.NULL -> "null";
            default -> "unknown";
        };
    }

    public static class NodeTypeTree {
        public final NodeTypeTree[] children;
        public final int currentType;


        public NodeTypeTree(int currentType, NodeTypeTree ...children) {
            this.currentType = currentType;
            this.children = children;
        }
    }

    public static void savePosition(Node node, AstNode old) {
        if (old == null) {
            return;
        }

        node.putIntProp(ExtendedConst.TOKEN_POSITION_PROP, old.getAbsolutePosition());
        node.putIntProp(ExtendedConst.TOKEN_LENGTH_PROP, old.getLength());
    }
    public static NodeTypeTree typeTree(int type, NodeTypeTree ...children) {
        return new NodeTypeTree(type, children);
    }
    public static NodeTypeTree[] typeTree(NodeTypeTree ...children) {
        return children;
    }

    public static boolean isTreeOf(Node node, NodeTypeTree[] tree) {
        Node it = node;
        int i = 0;
        while (it != null) {
            if (i >= tree.length) {
                return false;
            }
            NodeTypeTree n = tree[i];
            if (n.currentType >= 0 && n.currentType != it.getType()) {
                return false;
            }
            if(n.children.length > 0) {
                boolean match = hasTreeOf(it, n.children);
                if (!match) {
                    return false;
                }
            }
            it = it.getNext();
            i++;
        }
        return i == tree.length;
    }

    public static boolean hasTreeOf(Node node, NodeTypeTree[] tree) {
        Node last = node.getLastChild();
        Node it = node.getFirstChild();
        int i = 0;
        while (it != null) {
            if (i >= tree.length) {
                return false;
            }
            NodeTypeTree n = tree[i];
            if (n.currentType >= 0 && n.currentType != it.getType()) {
                return false;
            }
            if(n.children.length > 0) {
                boolean match = hasTreeOf(it, n.children);
                if (!match) {
                    return false;
                }
            }


            if (it == last) {
                i++;
                break;
            }
            it = it.getNext();
            i++;
        }
        return i == tree.length;
    }

    public static Node findFirstLocationalNode(Node node) {
        Node it = node;

        while (it != null) {
            int type = it.getType();
            if ((isLiteralOrIdentifier(it) || type == Token.GETVAR)) {
                return it;
            }
            if (type == Token.OBJECTLIT || type == Token.ARRAYLIT) {
                return it;
            }

            it = it.getFirstChild();
        }
        return null;
    }

    public static void forEachChildren(Node node, Consumer<Node> consumer) {
        Node last = node.getLastChild();
        Node it = node.getFirstChild();
        while (it != null) {
            consumer.accept(it);
            if (it == last) {
                break;
            }
            it = it.getNext();
        }
    }

    public static Node getFirstLeafChild(Node node) {
        Node it = node.getFirstChild();
        while (it != null) {
            if (it.getFirstChild() == null) {
                return it;
            }
            it = it.getFirstChild();
        }
        return null;
    }
}

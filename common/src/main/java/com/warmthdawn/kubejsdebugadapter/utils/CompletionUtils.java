package com.warmthdawn.kubejsdebugadapter.utils;

import com.warmthdawn.kubejsdebugadapter.debugger.KubeStackFrame;
import dev.latvian.mods.rhino.CompilerEnvirons;
import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.Parser;
import dev.latvian.mods.rhino.Undefined;
import dev.latvian.mods.rhino.ast.*;
import org.eclipse.lsp4j.debug.CompletionItem;
import org.eclipse.lsp4j.debug.CompletionItemType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CompletionUtils {
    public static List<CompletionItem> complete(String expression, KubeStackFrame stackFrame) {
        ErrorCollector errorCollector = new ErrorCollector();
        CompilerEnvirons env = new CompletionCompileEnv();
        env.setStrictMode(false);
        env.setErrorReporter(errorCollector);

        Parser parser = new Parser(env);
        AstRoot parse = parser.parse(expression, "<eval>", 1);

        List<String> candidateIdentifier = candidateIdentifier(stackFrame);
        List<CompletionItem> result = new ArrayList<>();

        parse.visit(node -> {
            if (node instanceof Name) {
                identifierCompleter((Name) node, candidateIdentifier, result);
            }

            if (node instanceof PropertyGet) {
                PropertyGet propertyGet = (PropertyGet) node;
                propertyCompleter(expression, stackFrame, propertyGet, result);
            }

            if (node instanceof ElementGet) {
                ElementGet elementGet = (ElementGet) node;
                elementCompleter(expression, stackFrame, elementGet, result);
            }

            return true;
        });


        return result;
    }

    private static List<String> candidateIdentifier(KubeStackFrame stackFrame) {
        List<String> result = new ArrayList<>();
        ContextFactory factory = stackFrame.getFactory();
        if (stackFrame.getThisObj() != null && stackFrame.getThisObj() != Undefined.instance) {
            result.add("this");
        }
        if (stackFrame.getScope() != null) {
            Object[] objectIds = VariableUtils.getObjectIds(factory, stackFrame.getScope());
            for (Object objectId : objectIds) {
                if (objectId instanceof String) {
                    result.add((String) objectId);
                }
            }
        }
        return result;


    }


    private static void identifierCompleter(Name node, List<String> candidates, List<CompletionItem> result) {
        String identifier = node.getIdentifier();
        int start = node.getPosition();
        int length = node.getLength();

        candidates.stream().filter(
            it -> it.startsWith(identifier)
        ).map(it -> {
            CompletionItem item = new CompletionItem();
            item.setLabel(it);
            item.setStart(start);
            item.setLength(length);
            return item;
        }).forEach(result::add);

    }


    private static void elementCompleter(String source, KubeStackFrame stackFrame, ElementGet node, List<CompletionItem> result) {

        AstNode element = node.getElement();
        int start = element.getPosition();
        int length = element.getLength();

        String expr;

        if (element instanceof ErrorNode) {
            expr = "";
            length = 0;
        } else {
            expr = source.substring(start, start + length);
        }

        boolean stringId = expr.startsWith("\"") || expr.startsWith("'");
        if (stringId) {
            if (expr.endsWith("\"") || expr.endsWith("'")) {
                expr = expr.substring(1, expr.length() - 1);
            } else {
                expr = expr.substring(1);
            }
        }

        ContextFactory factory = stackFrame.getFactory();

        AstNode target = node.getTarget();
        String prefix = source.substring(target.getPosition(), target.getPosition() + target.getLength());

        Object targetObj = EvalUtils.evaluate(factory, prefix, stackFrame.getScope());

        Object[] objectIds = VariableUtils.getObjectIds(factory, targetObj);
        for (Object objectId : objectIds) {
            if (objectId instanceof String) {
                String objectIdStr = (String) objectId;
                String text = "'" + objectIdStr + "'";

                if (!(stringId || expr.isEmpty()) || !objectIdStr.startsWith(expr)) {
                    continue;
                }


                CompletionItem item = new CompletionItem();
                item.setStart(start);
                item.setLength(length);
                item.setLabel(text);
                item.setType(CompletionItemType.TEXT);
                result.add(item);
            }
        }

    }

    private static void propertyCompleter(String source, KubeStackFrame stackFrame, PropertyGet node, List<CompletionItem> result) {


        int start = node.getProperty().getPosition();
        int length = node.getProperty().getLength();
        String identifier;
        try {
            identifier = source.substring(start, start + length);
        } catch (IndexOutOfBoundsException e) {
            identifier = null;
        }
        if (!Objects.equals(identifier, node.getProperty().getIdentifier())) {
            identifier = "";
            start = node.getOperatorPosition() + 1;
            length = 0;
        }


        ContextFactory factory = stackFrame.getFactory();

        AstNode target = node.getTarget();
        String prefix = source.substring(target.getPosition(), target.getPosition() + target.getLength());

        Object targetObj = EvalUtils.evaluate(factory, prefix, stackFrame.getScope());

        Object[] objectIds = VariableUtils.getObjectIds(factory, targetObj);
        for (Object objectId : objectIds) {
            if (objectId instanceof String && ((String) objectId).startsWith(identifier)) {
                CompletionItem item = new CompletionItem();
                item.setLabel((String) objectId);
                item.setType(CompletionItemType.PROPERTY);
                item.setStart(start);
                item.setLength(length);
                result.add(item);
            }
        }
    }
}

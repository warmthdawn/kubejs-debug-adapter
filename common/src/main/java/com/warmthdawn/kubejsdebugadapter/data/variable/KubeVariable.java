package com.warmthdawn.kubejsdebugadapter.data.variable;

import com.warmthdawn.kubejsdebugadapter.api.DebuggableScript;
import com.warmthdawn.kubejsdebugadapter.debugger.DebugSession;
import com.warmthdawn.kubejsdebugadapter.utils.PathUtil;
import com.warmthdawn.kubejsdebugadapter.utils.VariableUtils;
import dev.latvian.mods.rhino.ContextFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KubeVariable implements IVariableTreeNode {
    private static final Logger log = LogManager.getLogger();
    private Object obj;


    private String value;
    private int id;
    private String name;
    private String type;
    private ContextFactory factory;

    public KubeVariable(Object variable, int id, String name, ContextFactory factory) {
        this.obj = variable;
        this.id = id;
        this.name = name;
        this.factory = factory;
    }


    public String getValue() {
        if (value == null) {
            if (obj instanceof DebuggableScript && ((DebuggableScript) obj).isFunction()) {
                if (((DebuggableScript) obj).getSourceName() != null) {
                    String source = PathUtil.getSourcePath(((DebuggableScript) obj).getSourceName()).toString();
                    value = "@ " + source + ":" + ((DebuggableScript) obj).firstLineNumber();
                }
            }
            if (value == null) {
                value = VariableUtils.variableToString(factory, obj);
            }
        }
        return value;
    }

    public String getType() {
        if (type == null) {
            type = VariableUtils.variableType(factory, obj);
        }
        return type;
    }

    public boolean isPrimitive() {
        return VariableUtils.isPrimitive(obj);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<IVariableTreeNode> getChildren(DebugSession session) {
        if (isPrimitive()) {
            return Collections.emptyList();
        }
        Object[] objectIds = VariableUtils.getObjectIds(factory, obj);
        List<IVariableTreeNode> children = new ArrayList<>();
        for (Object objectId : objectIds) {
            try {
                // TODO: 超时
                children.add(session.createVariable(
                        VariableUtils.getObjectProperty(factory, obj, objectId),
                        VariableUtils.variableToString(factory, objectId),
                        factory
                    )
                );
            } catch (Throwable e) {
                children.add(session.createError(e, VariableUtils.variableToString(factory, objectId), factory));
            }
        }
        return children;
    }
}

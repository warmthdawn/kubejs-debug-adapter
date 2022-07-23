package com.warmthdawn.kubejsdebugadapter.data.variable;

import org.eclipse.lsp4j.debug.VariablePresentationHintKind;

public class VariableDescriptor {
    private final String name;
    private final String kind;
    private final boolean lazy;

    private final boolean readonly;


    public VariableDescriptor(String name, String kind, boolean lazy, boolean readonly) {
        this.name = name;
        this.kind = kind;
        this.lazy = lazy;
        this.readonly = readonly;
    }


    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }

    public boolean isLazy() {
        return lazy;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public static VariableDescriptor create(String name, String kind, boolean lazy, boolean readonly) {
        return new VariableDescriptor(name, kind, lazy, readonly);
    }

    public static VariableDescriptor createVar(String name) {
        return new VariableDescriptor(name, VariablePresentationHintKind.PROPERTY, false, false);
    }

    public static VariableDescriptor createNormal(String name) {
        return new VariableDescriptor(name, null, false, false);
    }

    public static VariableDescriptor createReadonly(String name) {
        return new VariableDescriptor(name, VariablePresentationHintKind.PROPERTY, false, true);
    }

    public static VariableDescriptor createField(String name, boolean readonly) {
        return new VariableDescriptor(name, VariablePresentationHintKind.PROPERTY, false, readonly);
    }
    public static VariableDescriptor createProperty(String name, boolean lazy, boolean readonly) {
        return new VariableDescriptor(name, VariablePresentationHintKind.PROPERTY, lazy, readonly);
    }

    public static VariableDescriptor createLazy(String name) {
        return new VariableDescriptor(name, VariablePresentationHintKind.PROPERTY, true, false);
    }

    public static VariableDescriptor createLazy(String name, boolean readonly) {
        return new VariableDescriptor(name, VariablePresentationHintKind.PROPERTY, true, readonly);
    }

    public static VariableDescriptor createMethod(String name) {
        return new VariableDescriptor(name, VariablePresentationHintKind.METHOD, false, true);
    }


    public static VariableDescriptor createClazz(String name) {
        return new VariableDescriptor(name, VariablePresentationHintKind.CLASS, false, true);
    }
}

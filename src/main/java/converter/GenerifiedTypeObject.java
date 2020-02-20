package main.java.converter;

import main.java.parser.OpSpTypeNode;

import java.util.List;

public class GenerifiedTypeObject implements NameableType {
    private StdTypeObject parent;
    private List<TypeObject> generics;

    public GenerifiedTypeObject(StdTypeObject parent, List<TypeObject> generics) {
        this.parent = parent;
        this.generics = generics;
    }

    @Override
    public boolean isSubclass(TypeObject other) {  // TODO: Check generics
        return parent.isSubclass(other);
    }

    @Override
    public TypeObject operatorReturnType(OpSpTypeNode o) {
        var parentReturn = parent.operatorReturnType(o);
        if (parentReturn == null) return null;
        if (parentReturn instanceof TemplateParam) {
            return generics.get(((TemplateParam) parentReturn).getIndex());
        } else {
            return parentReturn;
        }
    }

    @Override
    public String name() {
        return null;
    }
}

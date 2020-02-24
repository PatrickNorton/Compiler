package main.java.converter;

import main.java.parser.OpSpTypeNode;

import java.util.List;
import java.util.StringJoiner;

public class GenerifiedTypeObject implements NameableType {
    private StdTypeObject parent;
    private List<TypeObject> generics;

    public GenerifiedTypeObject(StdTypeObject parent, List<TypeObject> generics) {
        this.parent = parent;
        this.generics = generics;
    }

    @Override
    public boolean isSubclass(TypeObject other) {  // TODO: Check generics
        if (other instanceof GenerifiedTypeObject) {
            return parent.isSubclass(((GenerifiedTypeObject) other).parent);
        }
        return parent.isSubclass(other);
    }

    @Override
    public TypeObject attrType(String value) {
        var parentReturn = parent.attrTypeWithGenerics(value);
        if (parentReturn == null) return null;
        if (parentReturn instanceof TemplateParam) {
            return generics.get(((TemplateParam) parentReturn).getIndex());
        } else {
            return parentReturn;
        }
    }

    @Override
    public TypeObject operatorReturnType(OpSpTypeNode o) {
        var parentReturn = parent.operatorReturnTypeWithGenerics(o);
        if (parentReturn == null) return null;
        if (parentReturn instanceof TemplateParam) {
            return generics.get(((TemplateParam) parentReturn).getIndex());
        } else {
            return parentReturn;
        }
    }

    @Override
    public String name() {
        var valueJoiner = new StringJoiner("[", ", ", "]");
        for (var cls : generics) {
            valueJoiner.add(cls.name());
        }
        return parent + valueJoiner.toString();
    }
}

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
    public boolean isSuperclass(TypeObject other) {  // TODO: Check generics
        if (other instanceof GenerifiedTypeObject) {
            return parent.isSuperclass(((GenerifiedTypeObject) other).parent);
        }
        return parent.isSuperclass(other);
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
    public FunctionInfo operatorInfo(OpSpTypeNode o) {
        return parent.trueOperatorInfo(o).generify(generics);
    }

    @Override
    public TypeObject[] operatorReturnType(OpSpTypeNode o) {
        var parentReturn = parent.operatorReturnTypeWithGenerics(o);
        if (parentReturn == null) return null;
        TypeObject[] result = new TypeObject[parentReturn.length];
        for (int i = 0; i < parentReturn.length; i++) {
            if (parentReturn[i] instanceof TemplateParam) {
                result[i] = generics.get(((TemplateParam) parentReturn[i]).getIndex());
            } else {
                result[i] = parentReturn[i];
            }
        }
        return result;
    }

    @Override
    public String name() {
        var valueJoiner = new StringJoiner(", ", "[", "]");
        for (var cls : generics) {
            valueJoiner.add(cls.name());
        }
        return parent.name() + valueJoiner.toString();
    }
}

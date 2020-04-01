package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public final class GenerifiedTypeObject extends NameableType {
    private StdTypeObject parent;
    private List<TypeObject> generics;
    private String typedefName;

    public GenerifiedTypeObject(StdTypeObject parent, List<TypeObject> generics) {
        this.parent = parent;
        this.generics = generics;
        this.typedefName = "";
    }

    private GenerifiedTypeObject(StdTypeObject parent, List<TypeObject> generics, String typedefName) {
        this.parent = parent;
        this.generics = generics;
        this.typedefName = typedefName;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {  // TODO: Check generics
        if (other instanceof GenerifiedTypeObject) {
            return parent.isSuperclass(((GenerifiedTypeObject) other).parent);
        }
        return parent.isSuperclass(other);
    }

    @Override
    public boolean isSubclass(@NotNull TypeObject other) {
        return equals(other) || (!other.superWillRecurse() && other.isSuperclass(this));
    }

    public boolean subWillRecurse() {
        return true;
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
        if (!typedefName.isEmpty()) {
            return typedefName;
        }
        var valueJoiner = new StringJoiner(", ", "[", "]");
        for (var cls : generics) {
            valueJoiner.add(cls.name());
        }
        return parent.name() + valueJoiner.toString();
    }

    @Override
    public TypeObject typedefAs(String name) {
        return new GenerifiedTypeObject(parent, generics, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenerifiedTypeObject that = (GenerifiedTypeObject) o;
        return Objects.equals(parent, that.parent) &&
                Objects.equals(generics, that.generics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, generics);
    }
}

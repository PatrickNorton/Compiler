package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StdTypeObject implements NameableType {
    private final String name;
    private final List<TypeObject> supers;
    private final Map<OpSpTypeNode, FunctionInfo> operators;
    private final GenericInfo info;
    private Map<String, TypeObject> attributes;

    public StdTypeObject(String name) {
        this(name, Collections.emptyList());
    }

    public StdTypeObject(String name, List<TypeObject> supers) {
        this.name = name;
        this.supers = Collections.unmodifiableList(supers);
        this.operators = new EnumMap<>(OpSpTypeNode.class);
        this.info = GenericInfo.empty();
    }

    public StdTypeObject(String name, GenericInfo info) {
        this(name, Collections.emptyList(), info);
    }

    public StdTypeObject(String name, List<TypeObject> supers, GenericInfo info) {
        this.name = name;
        this.supers = Collections.unmodifiableList(supers);
        this.operators = new EnumMap<>(OpSpTypeNode.class);
        this.info = info;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        if (other instanceof TypedefType) {
            return isSuperclass(((TypedefType) other).getBase());
        }
        if (this.equals(other)) {
            return true;
        } else if (!(other instanceof StdTypeObject)) {  // TODO: When is this not true?
            return false;
        }
        for (var sup : ((StdTypeObject) other).supers) {
            if (sup.isSuperclass(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public TypeObject generify(@NotNull TypeObject... args) {
        if (args.length != info.getParams().size()) {
            throw new UnsupportedOperationException("Cannot generify object in this manner");
        } else {
            return new GenerifiedTypeObject(this, List.of(args));
        }
    }

    public GenericInfo getGenericInfo() {
        return info;
    }

    public void setOperator(OpSpTypeNode o, FunctionInfo args) {
        operators.put(o, args);
    }

    @Override
    public TypeObject operatorReturnType(OpSpTypeNode o) {
        var type = operatorReturnTypeWithGenerics(o);
        return type instanceof TemplateParam ? ((TemplateParam) type).getBound() : type;
    }

    @Nullable
    public TypeObject operatorReturnTypeWithGenerics(OpSpTypeNode o) {
        if (operators.containsKey(o)) {
            return operators.get(o).getReturns()[0];
        }
        for (var sup : supers) {
            var opRet = sup.operatorReturnType(o);
            if (opRet != null) {
                return opRet;
            }
        }
        return null;
    }

    public List<TypeObject> getSupers() {
        return supers;
    }

    @Override
    public TypeObject attrType(String value) {
        var type = attrTypeWithGenerics(value);
        return type instanceof TemplateParam ? ((TemplateParam) type).getBound() : type;
    }

    public TypeObject attrTypeWithGenerics(String value) {
        return attributes.get(value);
    }

    public void setAttributes(Map<String, TypeObject> attributes) {
        assert this.attributes == null;
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StdTypeObject that = (StdTypeObject) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(supers, that.supers) &&
                Objects.equals(operators, that.operators);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, supers, operators);
    }
}

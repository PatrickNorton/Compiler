package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StdTypeObject implements TypeObject {
    private final String name;
    private final List<TypeObject> supers;
    private final Map<OpSpTypeNode, FunctionInfo> operators;
    private final GenericInfo info;

    public StdTypeObject(String name) {
        this(name, Collections.emptyList());
    }

    public StdTypeObject(String name, List<TypeObject> supers) {
        this.name = name;
        this.supers = Collections.unmodifiableList(supers);
        this.operators = new EnumMap<>(OpSpTypeNode.class);
        this.info = GenericInfo.empty();
    }

    public boolean isSubclass(TypeObject other) {
        if (this.equals(other) || other instanceof ObjectType) {
            return true;
        }
        for (var sup : supers) {
            if (sup.isSubclass(other)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String name() {
        return name;
    }

    public void setOperator(OpSpTypeNode o, FunctionInfo args) {
        operators.put(o, args);
    }

    @Override
    @Nullable
    public TypeObject operatorReturnType(OpSpTypeNode o) {
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

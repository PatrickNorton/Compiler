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
    private final List<TypeObject> declaredGenerics;
    private final List<String> generics;
    private final Map<OpSpTypeNode, FunctionInfo> operators;

    public StdTypeObject(String name) {
        this(name, Collections.emptyList(), Collections.emptyList());
    }

    public StdTypeObject(String name, List<TypeObject> supers, Map<OpSpTypeNode, FunctionInfo> operators) {
        this.name = name;
        this.supers = Collections.unmodifiableList(supers);
        this.declaredGenerics = Collections.emptyList();
        this.generics = Collections.emptyList();
        this.operators = Collections.unmodifiableMap(operators);
    }

    public StdTypeObject(String name, List<TypeObject> supers, List<TypeObject> declaredGenerics) {
        this.name = name;
        this.supers = Collections.unmodifiableList(supers);
        this.declaredGenerics = Collections.unmodifiableList(declaredGenerics);
        this.generics = Collections.emptyList();
        this.operators = new EnumMap<>(OpSpTypeNode.class);
    }

    public StdTypeObject(String name, List<StdTypeObject> supers, List<String> generics, Object sentinel) {
        this.name = name;
        this.supers = Collections.unmodifiableList(supers);
        this.generics = Collections.unmodifiableList(generics);
        this.declaredGenerics = Collections.emptyList();
        this.operators = new EnumMap<>(OpSpTypeNode.class);
        assert sentinel == null;
    }

    public boolean isSubclass(TypeObject other) {
        if (this.equals(other)) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StdTypeObject that = (StdTypeObject) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(declaredGenerics, that.declaredGenerics) &&
                Objects.equals(generics, that.generics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, declaredGenerics, generics);
    }
}

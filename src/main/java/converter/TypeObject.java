package main.java.converter;

import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

public interface TypeObject extends LangObject {
    boolean isSubclass(TypeObject other);
    String name();

    default TypeObject getType() {
        return new TypeTypeObject();
    }

    default TypeObject operatorReturnType(OperatorTypeNode o) {
        return operatorReturnType(OpSpTypeNode.translate(o));
    }

    default TypeObject operatorReturnType(OpSpTypeNode o) {
        return null;
    }

    static TypeObject union(TypeObject... values) {
        SortedSet<TypeObject> sortedSet = new TreeSet<>(Arrays.asList(values));
        assert !sortedSet.isEmpty();
        return sortedSet.size() == 1 ? sortedSet.first() : new UnionTypeObject(sortedSet);
    }

    static TypeObject intersection(TypeObject... values) {
        SortedSet<TypeObject> sortedSet = new TreeSet<>(Arrays.asList(values));
        assert !sortedSet.isEmpty();
        return sortedSet.size() == 1 ? sortedSet.first() : new IntersectionTypeObject(sortedSet);
    }

    @NotNull
    @Contract(pure = true)
    static TypeObject optional(@NotNull TypeObject value) {
        return value instanceof OptionalTypeObject ? value : new OptionalTypeObject(value);
    }
}

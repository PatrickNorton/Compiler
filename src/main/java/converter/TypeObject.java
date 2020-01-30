package main.java.converter;

import main.java.parser.OpSpTypeNode;
import main.java.parser.OperatorTypeNode;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

public interface TypeObject {
    boolean isSubclass(TypeObject other);
    String name();

    default TypeObject operatorReturnType(OperatorTypeNode o) {
        return operatorReturnType((OpSpTypeNode) null);  // TODO: Make it return correctly
    }

    default TypeObject operatorReturnType(OpSpTypeNode o) {
        return null;
    }

    static TypeObject union(TypeObject... values) {
        SortedSet<TypeObject> sortedSet = new TreeSet<>(Arrays.asList(values));
        assert !sortedSet.isEmpty();
        if (sortedSet.size() == 1) {
            return sortedSet.first();
        } else {
            return new UnionTypeObject(sortedSet);
        }
    }
}

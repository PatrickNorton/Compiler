package main.java.converter;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

public interface TypeObject {
    boolean isSubclass(TypeObject other);
    String name();

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

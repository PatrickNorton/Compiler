package main.java.converter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;

public class UnionTypeObject implements TypeObject {
    private final SortedSet<TypeObject> types;

    public UnionTypeObject(SortedSet<TypeObject> types) {
        this.types = Collections.unmodifiableSortedSet(types);
    }

    public UnionTypeObject(TypeObject... types) {
        this.types = Collections.unmodifiableSortedSet(new TreeSet<>(List.of(types)));
    }

    @Override
    public String name() {
        var sj = new StringJoiner("|");
        for (var type : types) {
            sj.add(type.name());
        }
        return sj.toString();
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        for (var subtype : types) {
            if (subtype.isSuperclass(other)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnionTypeObject that = (UnionTypeObject) o;
        return Objects.equals(types, that.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(types);
    }
}

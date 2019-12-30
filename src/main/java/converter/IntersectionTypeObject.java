package main.java.converter;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class IntersectionTypeObject implements TypeObject {
    private final List<TypeObject> types;

    public IntersectionTypeObject(TypeObject... types) {
        this.types = List.of(types);
    }

    @Override
    public String name() {
        var sj = new StringJoiner("&");
        for (var type : types) {
            sj.add(type.name());
        }
        return sj.toString();
    }

    @Override
    public boolean isSubclass(TypeObject other) {
        for (var subtype : types) {
            if (!subtype.isSubclass(other)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntersectionTypeObject that = (IntersectionTypeObject) o;
        return Objects.equals(types, that.types);
    }

    @Override
    public int hashCode() {
        return Objects.hash(types);
    }
}

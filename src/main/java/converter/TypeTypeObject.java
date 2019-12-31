package main.java.converter;

import java.util.Objects;

public class TypeTypeObject implements TypeObject {
    @Override
    public boolean isSubclass(TypeObject other) {
        return other instanceof TypeTypeObject;
    }

    @Override
    public String name() {
        return "type";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TypeTypeObject;
    }

    @Override
    public int hashCode() {
        return Objects.hash(TypeTypeObject.class, "type");
    }
}

package main.java.converter;

import java.util.Objects;

public class ObjectType implements TypeObject {
    @Override
    public boolean isSuperclass(TypeObject other) {
        return true;
    }

    @Override
    public String name() {
        return "object";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ObjectType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ObjectType.class);
    }
}

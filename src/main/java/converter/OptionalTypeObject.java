package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class OptionalTypeObject implements TypeObject {
    private TypeObject type;

    public OptionalTypeObject(@NotNull TypeObject type) {
        this.type = type.stripNull();
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        return type.isSuperclass(other) || Builtins.NULL_TYPE.isSuperclass(other) || equals(other);
    }

    @Override
    public String name() {
        return type instanceof StdTypeObject
                ? type.name() + "?"
                : String.format("(%s)?", type.name());
    }

    @Override
    public TypeObject stripNull() {
        return type.stripNull();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptionalTypeObject that = (OptionalTypeObject) o;
        return Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }
}

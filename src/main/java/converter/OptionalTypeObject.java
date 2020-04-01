package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class OptionalTypeObject extends TypeObject {
    private final TypeObject type;
    private final String typedefName;

    public OptionalTypeObject(@NotNull TypeObject type) {
        this.type = type.stripNull();
        this.typedefName = "";
    }

    private OptionalTypeObject(TypeObject type, String typedefName) {
        this.type = type;
        this.typedefName = typedefName;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        return type.isSuperclass(other) || Builtins.NULL_TYPE.isSuperclass(other) || equals(other);
    }

    @Override
    public boolean isSubclass(@NotNull TypeObject other) {
        return type.isSubclass(other) && Builtins.NULL_TYPE.isSubclass(other);
    }

    @Override
    public String name() {
        if (!typedefName.isEmpty()) {
            return typedefName;
        }
        return type instanceof StdTypeObject
                ? type.name() + "?"
                : String.format("(%s)?", type.name());
    }

    @NotNull
    @Contract("_ -> new")
    @Override
    public TypeObject typedefAs(String name) {
        return new OptionalTypeObject(type, name);
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

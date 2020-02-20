package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TypeTypeObject implements TypeObject {
    @Override
    public boolean isSubclass(TypeObject other) {
        return other instanceof TypeTypeObject
                || other instanceof ObjectType
                || other instanceof GenerifiedTypeTypeObject;
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

    @Override
    public TypeObject generify(@NotNull TypeObject... args) {
        assert args.length == 1;
        return new GenerifiedTypeTypeObject(args[0]);
    }
}

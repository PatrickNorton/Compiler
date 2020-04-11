package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TypeTypeObject implements TypeObject {
    @Override
    public boolean isSuperclass(TypeObject other) {
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

    @Override
    public TypeObject[] operatorReturnType(OpSpTypeNode o) {
        if (o == OpSpTypeNode.CALL) {
            return new TypeObject[] {Builtins.OBJECT};
        } else {
            throw new UnsupportedOperationException("Cannot get type");
        }
    }
}

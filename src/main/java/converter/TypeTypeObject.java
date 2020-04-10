package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class TypeTypeObject extends TypeObject {
    private final String typedefName;

    public TypeTypeObject() {
        this.typedefName = "";
    }

    private TypeTypeObject(String typedefName) {
        this.typedefName = typedefName;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        return other instanceof TypeTypeObject
                || other instanceof ObjectType
                || other instanceof GenerifiedTypeTypeObject;
    }

    @Override
    public boolean isSubclass(@NotNull TypeObject other) {
        return other instanceof TypeTypeObject || other instanceof ObjectType;
    }

    @Override
    public String name() {
        return typedefName.isEmpty() ? "type" : typedefName;
    }

    @Override
    public TypeObject typedefAs(String name) {
        return new TypeTypeObject(name);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TypeTypeObject;
    }

    @Override
    public int hashCode() {
        return Objects.hash(TypeTypeObject.class, "type");
    }

    @Contract("_ -> new")
    @Override
    @NotNull
    public TypeObject generify(@NotNull TypeObject... args) {
        assert args.length == 1;
        return new GenerifiedTypeTypeObject(args[0]);
    }

    @Contract(value = "_, _ -> new", pure = true)
    @Override
    @NotNull
    public TypeObject[] operatorReturnType(OpSpTypeNode o, DescriptorNode access) {
        assert access == DescriptorNode.PUBLIC : "Should never have private access to 'type'";
        if (o == OpSpTypeNode.CALL) {
            return new TypeObject[] {Builtins.OBJECT};
        } else {
            throw new UnsupportedOperationException("Cannot get type");
        }
    }
}

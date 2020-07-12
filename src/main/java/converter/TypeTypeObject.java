package main.java.converter;

import main.java.parser.DescriptorNode;
import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class TypeTypeObject extends TypeObject {
    private final String typedefName;
    private final TypeObject generic;

    public TypeTypeObject() {
        this.typedefName = "";
        this.generic = null;
    }

    private TypeTypeObject(TypeObject generic, String typedefName) {
        this.generic = generic;
        this.typedefName = typedefName;
    }

    public TypeObject representedType() {
        return generic;
    }

    @Override
    public boolean isSuperclass(@NotNull TypeObject other) {
        return other instanceof TypeTypeObject
                || other instanceof ObjectType;
    }

    @Override
    protected boolean isSubclass(@NotNull TypeObject other) {
        return other instanceof TypeTypeObject || other instanceof ObjectType;
    }

    @Override
    public String name() {
        return typedefName.isEmpty() ? "type" : typedefName;
    }

    @Contract("_ -> new")
    @Override
    @NotNull
    public TypeObject typedefAs(String name) {
        return new TypeTypeObject(this.generic, name);
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
        return new TypeTypeObject(args[0], this.typedefName);
    }

    @Contract(value = "_, _ -> new", pure = true)
    @Override
    @NotNull
    public TypeObject[] operatorReturnType(OpSpTypeNode o, DescriptorNode access) {
        assert access == DescriptorNode.PUBLIC : "Should never have private access to 'type'";
        if (o == OpSpTypeNode.CALL) {
            return new TypeObject[] {generic == null ? Builtins.OBJECT : generic.makeMut()};
        } else {
            throw new UnsupportedOperationException("Cannot get type");
        }
    }

    @Override
    @Nullable
    public FunctionInfo operatorInfo(OpSpTypeNode o, DescriptorNode access) {
        if (generic != null) {
            if (o == OpSpTypeNode.CALL) {
                var opInfo = generic.operatorInfo(OpSpTypeNode.NEW, access);
                if (opInfo == null) {
                    return null;
                } else {
                    return new FunctionInfo(generic.operatorInfo(OpSpTypeNode.NEW, access).getArgs(), generic);
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    @Nullable
    public TypeObject attrType(String value, DescriptorNode access) {
        return generic == null ? null : generic.staticAttrType(value, access);
    }
}

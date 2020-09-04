package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class ObjectType extends TypeObject {
    private final String typedefName;

    public ObjectType() {
        this.typedefName = "";
    }

    private ObjectType(String typedefName) {
        this.typedefName = typedefName;
    }

    @Override
    public boolean isSuperclass(@NotNull TypeObject other) {
        return true;
    }

    protected boolean isSubclass(@NotNull TypeObject other) {
        return other.equals(this);
    }

    @Override
    @Nullable
    public FunctionInfo operatorInfo(@NotNull OpSpTypeNode o, AccessLevel access) {
        switch (o) {
            case EQUALS:
                return new FunctionInfo(ArgumentInfo.of(Builtins.OBJECT), Builtins.BOOL);
            case STR:
                return new FunctionInfo(ArgumentInfo.of(), Builtins.STR);
            default:
                return null;
        }
    }

    @Override
    public String name() {
        return typedefName.isEmpty() ? "object" : typedefName;
    }

    @Contract(pure = true)
    @Override
    @NotNull
    public String baseName() {
        return "object";
    }

    @Contract("_ -> new")
    @Override
    @NotNull
    public TypeObject typedefAs(String name) {
        return new ObjectType(name);
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

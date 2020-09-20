package main.java.converter;

import main.java.parser.OpSpTypeNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
    @NotNull
    public Optional<FunctionInfo> operatorInfo(@NotNull OpSpTypeNode o, AccessLevel access) {
        switch (o) {
            case EQUALS:
                return Optional.of(new FunctionInfo(ArgumentInfo.of(Builtins.OBJECT), Builtins.BOOL));
            case STR:
                return Optional.of(new FunctionInfo(ArgumentInfo.of(), Builtins.STR));
            default:
                return Optional.empty();
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
    public boolean sameBaseType(TypeObject other) {
        return other instanceof ObjectType;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ObjectType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ObjectType.class);
    }

    @Override
    public Optional<Map<Integer, TypeObject>> generifyAs(TypeObject parent, TypeObject other) {
        if (other instanceof ObjectType) {
            return Optional.of(new HashMap<>());
        } else {
            return Optional.empty();
        }
    }
}

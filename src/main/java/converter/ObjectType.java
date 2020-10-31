package main.java.converter;

import main.java.parser.OpSpTypeNode;

import java.util.Collections;
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
    public boolean isSuperclass(TypeObject other) {
        return true;
    }

    @Override
    public boolean willSuperRecurse() {
        return false;
    }

    protected boolean isSubclass(TypeObject other) {
        return other.equals(this);
    }

    private static final FunctionInfo EQUALS_INFO = new FunctionInfo(ArgumentInfo.of(Builtins.OBJECT), Builtins.BOOL);
    private static final FunctionInfo STR_INFO = new FunctionInfo(ArgumentInfo.of(), Builtins.STR);

    @Override
    public Optional<FunctionInfo> operatorInfo(OpSpTypeNode o, AccessLevel access) {
        switch (o) {
            case EQUALS:
                return Optional.of(EQUALS_INFO);
            case STR:
            case REPR:
                return Optional.of(STR_INFO);
            default:
                return Optional.empty();
        }
    }

    @Override
    public String name() {
        return typedefName.isEmpty() ? "object" : typedefName;
    }

    @Override
    public String baseName() {
        return "object";
    }

    @Override
    public TypeObject typedefAs(String name) {
        return new ObjectType(name);
    }

    @Override
    public boolean sameBaseType(TypeObject other) {
        return other instanceof ObjectType;
    }

    @Override
    public int baseHash() {
        return baseName().hashCode();
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
        return Optional.of(Collections.emptyMap());
    }
}

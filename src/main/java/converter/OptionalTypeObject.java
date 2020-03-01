package main.java.converter;

public class OptionalTypeObject implements TypeObject {
    private TypeObject type;

    public OptionalTypeObject(TypeObject type) {
        this.type = type instanceof OptionalTypeObject
                ? ((OptionalTypeObject) type).type
                : type;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        return type.isSuperclass(other) || Builtins.NULL_TYPE.isSuperclass(other);
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
}

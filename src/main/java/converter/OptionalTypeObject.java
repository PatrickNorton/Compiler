package main.java.converter;

public class OptionalTypeObject implements TypeObject {
    private TypeObject type;

    public OptionalTypeObject(TypeObject type) {
        this.type = type instanceof OptionalTypeObject
                ? ((OptionalTypeObject) type).type
                : type;
    }

    @Override
    public boolean isSubclass(TypeObject other) {
        return type.isSubclass(other);
    }

    @Override
    public String name() {
        return type instanceof StdTypeObject
                ? type.name() + "?"
                : String.format("(%s)?", type.name());
    }
}

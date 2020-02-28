package main.java.converter;

public class TypedefType implements NameableType {
    private TypeObject internal;
    private String name;

    public TypedefType(String name, TypeObject internal) {
        this.internal = internal;
        this.name = name;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        return internal.isSuperclass(other);
    }

    @Override
    public String name() {
        return name;
    }
}

package main.java.converter;

public final class TemplateParam implements NameableType {
    private String name;
    private int index;
    private TypeObject bound;

    public TemplateParam(String name, int index, TypeObject bound) {
        this.name = name;
        this.index = index;
        this.bound = bound;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        return bound.isSuperclass(other);
    }

    @Override
    public String name() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public TypeObject getBound() {
        return bound;
    }
}

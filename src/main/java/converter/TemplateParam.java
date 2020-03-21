package main.java.converter;

public final class TemplateParam implements NameableType {
    private String name;
    private int index;
    private TypeObject bound;
    private boolean isVararg;

    public TemplateParam(String name, int index, boolean isVararg) {
        this(name, index, TypeObject.list(), true);
        assert isVararg;
    }

    public TemplateParam(String name, int index, TypeObject bound) {
        this(name, index, bound, false);
    }

    private TemplateParam(String name, int index, TypeObject bound, boolean isVararg) {
        this.name = name;
        this.index = index;
        this.bound = bound;
        this.isVararg = isVararg;
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

    public boolean isVararg() {
        return isVararg;
    }
}

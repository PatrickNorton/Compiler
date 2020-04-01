package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class TemplateParam extends NameableType {
    private String name;
    private int index;
    private TypeObject bound;
    private boolean isVararg;
    private String typedefName;

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

    @Contract(pure = true)
    private TemplateParam(@NotNull TemplateParam other, String typedefName) {
        this.name = other.name;
        this.index = other.index;
        this.bound = other.bound;
        this.isVararg = other.isVararg;
        this.typedefName = typedefName;
    }

    @Override
    public boolean isSuperclass(TypeObject other) {
        return bound.isSuperclass(other);
    }

    @Override
    public boolean isSubclass(@NotNull TypeObject other) {
        return equals(other) || (!other.subWillRecurse() && other.isSuperclass(this));
    }

    @Override
    public boolean subWillRecurse() {
        return true;
    }

    @Override
    public String name() {
        return typedefName.isEmpty() ? name : typedefName;
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    @Override
    public TypeObject typedefAs(String name) {
        return new TemplateParam(this, name);
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

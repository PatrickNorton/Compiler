package main.java.converter;

import main.java.util.IndexedSet;
import main.java.util.OptionalBool;

import java.util.List;
import java.util.Objects;

public final class BoolConstant implements LangConstant {
    private final boolean value;

    public BoolConstant(boolean value) {
        this.value = value;
    }

    @Override
    public List<Byte> toBytes() {
        return List.of((byte) ConstantBytes.BOOL.ordinal(), (byte) (value ? 1 : 0));
    }

    @Override
    public TypeObject getType() {
        return Builtins.BOOL;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public OptionalBool boolValue() {
        return OptionalBool.of(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BoolConstant that = (BoolConstant) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String name(IndexedSet<LangConstant> constants) {
        return Boolean.toString(value);
    }
}

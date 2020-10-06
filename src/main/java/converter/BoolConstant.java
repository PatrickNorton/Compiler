package main.java.converter;

import main.java.util.OptionalBool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Objects;

public final class BoolConstant implements LangConstant {
    private final boolean value;

    public BoolConstant(boolean value) {
        this.value = value;
    }

    @NotNull
    @Override
    @Unmodifiable
    public List<Byte> toBytes() {
        return List.of((byte) ConstantBytes.BOOL.ordinal(), (byte) (value ? 1 : 0));
    }

    @NotNull
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

    @NotNull
    @Override
    public String name() {
        return Boolean.toString(value);
    }
}

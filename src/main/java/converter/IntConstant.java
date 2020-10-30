package main.java.converter;

import main.java.util.IndexedSet;
import main.java.util.OptionalBool;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class IntConstant implements LangConstant {
    private final int value;

    public IntConstant(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntConstant that = (IntConstant) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(5);
        bytes.add((byte) ConstantBytes.INT.ordinal());
        bytes.addAll(Util.intToBytes(value));
        return bytes;
    }

    @Override
    public TypeObject getType() {
        return Builtins.INT;
    }

    @Override
    public String name(IndexedSet<LangConstant> constants) {
        return Integer.toString(value);
    }

    public int getValue() {
        return value;
    }

    @Override
    public OptionalBool boolValue() {
        return OptionalBool.of(value != 0);
    }
}

package main.java.converter;

import java.util.List;
import java.util.Objects;

public class BoolConstant implements LangConstant {
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
}

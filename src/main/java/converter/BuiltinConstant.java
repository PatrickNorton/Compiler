package main.java.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BuiltinConstant implements LangConstant {
    private final int builtinIndex;

    public BuiltinConstant(int index) {
        builtinIndex = index;
    }

    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(1 + Integer.BYTES);
        bytes.add((byte) ConstantBytes.BIGINT.ordinal());
        bytes.addAll(Util.intToBytes(builtinIndex));
        return bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuiltinConstant that = (BuiltinConstant) o;
        return builtinIndex == that.builtinIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(builtinIndex);
    }
}
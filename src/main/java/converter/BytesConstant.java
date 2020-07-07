package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BytesConstant implements LangConstant {
    private final List<Byte> value;

    public BytesConstant(List<Byte> value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BytesConstant that = (BytesConstant) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @NotNull
    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(value.size() + Integer.BYTES + 1);  // Guess capacity
        bytes.add((byte) ConstantBytes.BYTES.ordinal());
        bytes.addAll(Util.intToBytes(value.size()));
        bytes.addAll(value);
        return bytes;
    }

    @NotNull
    @Override
    public TypeObject getType() {
        return Builtins.BYTES;
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String name() {
        StringBuilder result = new StringBuilder();
        for (var b : value) {
            result.append((char) b.byteValue());
        }
        return "b\"" + result + '"';
    }
}
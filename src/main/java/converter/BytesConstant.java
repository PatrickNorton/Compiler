package main.java.converter;

import main.java.util.IndexedSet;
import main.java.util.OptionalBool;
import main.java.util.StringEscape;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BytesConstant implements LangConstant {
    private final List<Byte> value;

    public BytesConstant(List<Byte> value) {
        this.value = value;
    }

    public List<Byte> getValue() {
        return value;
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

    @Override
    public OptionalBool boolValue() {
        return OptionalBool.of(!value.isEmpty());
    }

    @Override
    public Optional<String> reprValue() {
        StringBuilder result = new StringBuilder(value.size());
        for (var b : value) {
            // Since Java doesn't have unsigned bytes, ASCII characters are >= 0 and non-ASCII characters are < 0
            if (b >= 0) {
                result.append(b);
            } else {
                var proper = Byte.toUnsignedInt(b);
                result.append("\\x").append(Integer.toHexString(proper));
            }
        }
        return Optional.of(result.toString());
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String name(IndexedSet<LangConstant> constants) {
        StringBuilder result = new StringBuilder();
        for (var b : value) {
            result.append(StringEscape.escaped((char) b.byteValue()));
        }
        return "b\"" + result + '"';
    }
}

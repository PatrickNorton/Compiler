package main.java.converter;

import main.java.util.IndexedSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BuiltinConstant implements LangConstant {
    private final int builtinIndex;

    public BuiltinConstant(int index) {
        builtinIndex = index;
    }

    @NotNull
    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(1 + Integer.BYTES);
        bytes.add((byte) ConstantBytes.BUILTIN.ordinal());
        bytes.addAll(Util.intToBytes(builtinIndex));
        return bytes;
    }

    @NotNull
    @Override
    public TypeObject getType() {
        return Builtins.TRUE_BUILTINS.get(builtinIndex).getType();
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

    @NotNull
    @Override
    public String name(IndexedSet<LangConstant> constants) {
        return name();
    }

    @Override
    public Optional<String> strValue() {
        return Optional.of(name());
    }

    private String name() {
        var result = Builtins.TRUE_BUILTINS.get(builtinIndex);
        if (result == Builtins.NULL_TYPE) {
            return "type(null)";
        }
        for (var pair : Builtins.BUILTIN_MAP.entrySet()) {
            if (pair.getValue() == result) {
                return pair.getKey();
            }
        }
        throw new RuntimeException();
    }
}

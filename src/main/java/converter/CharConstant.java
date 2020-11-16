package main.java.converter;

import main.java.util.IndexedSet;
import main.java.util.OptionalBool;
import main.java.util.StringEscape;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CharConstant implements LangConstant {
    private final char value;

    public CharConstant(char value) {
        this.value = value;
    }

    @Override
    @NotNull
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(1 + Character.BYTES);
        bytes.add((byte) ConstantBytes.CHAR.ordinal());
        bytes.addAll(Util.shortToBytes((short) value));
        return bytes;
    }

    @Override
    public OptionalBool boolValue() {
        return OptionalBool.of(value != 0);
    }

    @Override
    @NotNull
    public TypeObject getType() {
        return Builtins.CHAR;
    }

    @Override
    @NotNull
    public String name(IndexedSet<LangConstant> constants) {
        if (value == '\'') {
            return "c\"'\"";
        } else {
            return String.format("c'%s'", StringEscape.escaped(value));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharConstant that = (CharConstant) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

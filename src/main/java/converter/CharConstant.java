package main.java.converter;

import main.java.util.IndexedSet;
import main.java.util.OptionalBool;
import main.java.util.StringEscape;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class CharConstant implements LangConstant {
    private final int value;

    public CharConstant(int value) {
        this.value = value;
    }

    @Override
    @NotNull
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(1 + Integer.BYTES);
        bytes.add((byte) ConstantBytes.CHAR.ordinal());
        bytes.addAll(Util.intToBytes(value));
        return bytes;
    }

    @Override
    public OptionalBool boolValue() {
        return OptionalBool.of(value != 0);
    }

    @Override
    public Optional<String> strValue() {
        return Optional.of(new String(Character.toChars(value)));
    }

    @Override
    public Optional<String> reprValue() {
        return Optional.of(name());
    }

    @Override
    @NotNull
    public TypeObject getType() {
        return Builtins.charType();
    }

    @Override
    @NotNull
    public String name(IndexedSet<LangConstant> constants) {
        return name(value);
    }

    public String name() {
        return name(value);
    }

    public static String name(int value) {
        switch (value) {
            case '\'':
                return "c\"'\"";
            case '"':
                return "c'\"'";
            default:
                return String.format("c'%s'", StringEscape.escaped(value));
        }
    }

    public int getValue() {
        return value;
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

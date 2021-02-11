package main.java.converter;

import main.java.util.IndexedSet;
import main.java.util.OptionalBool;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ClassConstant implements LangConstant {
    private final String name;
    private final int index;
    private final UserType<?> type;

    public ClassConstant(String name, int index, UserType<?> type) {
        this.name = name;
        this.index = index;
        this.type = type;
    }

    @NotNull
    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(1 + Integer.BYTES);
        bytes.add((byte) ConstantBytes.CLASS.ordinal());
        bytes.addAll(Util.intToBytes(index));
        return bytes;
    }

    @NotNull
    @Override
    public TypeObject getType() {
        return Builtins.type().generify(type);
    }

    @NotNull
    @Override
    public String name(IndexedSet<LangConstant> constants) {
        return name;
    }

    @Override
    public OptionalBool boolValue() {
        return OptionalBool.of(true);
    }

    @Override
    public Optional<String> strValue() {
        return Optional.of(type.name());
    }

    @Override
    public Optional<String> reprValue() {
        return Optional.of(type.name());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassConstant that = (ClassConstant) o;
        return index == that.index &&
                Objects.equals(name, that.name) &&
                Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, index, type);
    }
}

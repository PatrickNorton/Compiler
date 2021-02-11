package main.java.converter;

import main.java.util.IndexedSet;
import main.java.util.OptionalBool;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public final class TupleConstant implements LangConstant {
    private final List<Pair<Short, TypeObject>> values;

    public TupleConstant(List<Pair<Short, TypeObject>> values) {
        this.values = values;
    }

    public List<Pair<Short, TypeObject>> getValues() {
        return values;
    }

    @Override
    @NotNull
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add((byte) ConstantBytes.TUPLE.ordinal());
        bytes.addAll(Util.intToBytes(values.size()));
        for (var pair : values) {
            bytes.addAll(Util.shortToBytes(pair.getKey()));
        }
        return bytes;
    }

    @Override
    @NotNull
    public TypeObject getType() {
        var generics = new TypeObject[values.size()];
        for (int i = 0; i < values.size(); i++) {
            generics[i] = values.get(i).getValue();
        }
        return Builtins.tuple().generify(generics);
    }

    @Override
    @NotNull
    public String name(IndexedSet<LangConstant> constants) {
        var joiner = new StringJoiner(", ", "(", ")");
        for (var pair : values) {
            joiner.add(constants.get(pair.getKey()).name(constants));
        }
        return joiner.toString();
    }

    @Override
    public OptionalBool boolValue() {
        return OptionalBool.of(!values.isEmpty());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TupleConstant that = (TupleConstant) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}

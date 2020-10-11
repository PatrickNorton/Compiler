package main.java.converter;

import main.java.util.OptionalBool;
import main.java.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class TupleConstant implements LangConstant {
    private final List<Pair<Short, TypeObject>> values;

    public TupleConstant(List<Pair<Short, TypeObject>> values) {
        this.values = values;
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
        return Builtins.TUPLE.generify(generics);
    }

    @Override
    @NotNull
    public String name() {
        return String.format("tuple[len=%d]", values.size());
    }

    @Override
    public OptionalBool boolValue() {
        return OptionalBool.of(!values.isEmpty());
    }
}

package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
        return Builtins.TYPE.generify(type);
    }

    @NotNull
    @Override
    public String name() {
        return name;
    }
}

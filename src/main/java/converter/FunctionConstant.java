package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class FunctionConstant implements LangConstant {
    private final String name;
    private final int functionIndex;

    public FunctionConstant(String name, int index) {
        this.name = name;
        this.functionIndex = index;
    }

    @NotNull
    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(1 + Integer.BYTES);
        bytes.add((byte) ConstantBytes.FUNCTION.ordinal());
        bytes.addAll(Util.intToBytes(functionIndex));
        return bytes;
    }

    @NotNull
    @Override
    public TypeObject getType() {
        return Builtins.CALLABLE;
    }

    @NotNull
    @Override
    public String name() {
        return name;
    }
}

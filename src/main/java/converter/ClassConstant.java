package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ClassConstant implements LangConstant {
    private int index;

    public ClassConstant(int index) {
        this.index = index;
    }

    @NotNull
    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(1 + Integer.BYTES);
        bytes.add((byte) ConstantBytes.CLASS.ordinal());
        bytes.addAll(Util.intToBytes(index));
        return bytes;
    }

    @Override
    public TypeObject getType() {
        return Builtins.TYPE;
    }
}

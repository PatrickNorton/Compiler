package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class IntConstant implements LangConstant {
    private int value;

    public IntConstant(int value) {
        this.value = value;
    }

    @NotNull
    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(5);
        bytes.add((byte) ConstantBytes.INT.ordinal());
        bytes.addAll(Util.intToBytes(value));
        return bytes;
    }
}

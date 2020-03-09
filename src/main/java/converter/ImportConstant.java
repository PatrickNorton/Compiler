package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ImportConstant implements LangConstant {
    private int index;

    public ImportConstant(int index) {
        this.index = index;
    }

    @NotNull
    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add((byte) ConstantBytes.IMPORT.ordinal());
        bytes.addAll(Util.intToBytes(index));
        return bytes;
    }

    @NotNull
    @Override
    public TypeObject getType() {
        throw new RuntimeException("Cannot figure out type of this yet");
    }
}

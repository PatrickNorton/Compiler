package main.java.converter;

import main.java.util.IndexedSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class ImportConstant implements LangConstant {
    private final String name;
    private final int index;

    public ImportConstant(int index, String name) {
        this.index = index;
        this.name = name;
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

    @Override
    public @NotNull String name(IndexedSet<LangConstant> constants) {
        return name;
    }
}

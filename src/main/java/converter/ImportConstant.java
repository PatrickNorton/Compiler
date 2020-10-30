package main.java.converter;

import main.java.util.IndexedSet;

import java.util.ArrayList;
import java.util.List;

public final class ImportConstant implements LangConstant {
    private final String name;
    private final int index;

    public ImportConstant(int index, String name) {
        this.index = index;
        this.name = name;
    }

    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add((byte) ConstantBytes.IMPORT.ordinal());
        bytes.addAll(Util.intToBytes(index));
        return bytes;
    }

    @Override
    public TypeObject getType() {
        throw new RuntimeException("Cannot figure out type of this yet");
    }

    @Override
    public String name(IndexedSet<LangConstant> constants) {
        return name;
    }
}

package main.java.converter;

import main.java.util.IndexedSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FunctionConstant implements LangConstant {
    private final String name;
    private final int functionIndex;

    public FunctionConstant(String name, int index) {
        this.name = name;
        this.functionIndex = index;
    }

    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(1 + Integer.BYTES);
        bytes.add((byte) ConstantBytes.FUNCTION.ordinal());
        bytes.addAll(Util.intToBytes(functionIndex));
        return bytes;
    }

    @Override
    public TypeObject getType() {
        return Builtins.CALLABLE;
    }

    @Override
    public String name(IndexedSet<LangConstant> constants) {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionConstant that = (FunctionConstant) o;
        return functionIndex == that.functionIndex &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, functionIndex);
    }
}

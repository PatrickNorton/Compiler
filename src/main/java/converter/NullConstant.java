package main.java.converter;

import main.java.util.IndexedSet;
import main.java.util.OptionalBool;

import java.util.List;

public final class NullConstant implements LangConstant {
    public static final StdTypeObject TYPE = new StdTypeObject("null");

    @Override
    public List<Byte> toBytes() {
        return List.of((byte) ConstantBytes.NULL.ordinal());
    }

    @Override
    public TypeObject getType() {
        return TYPE;
    }

    @Override
    public String name(IndexedSet<LangConstant> constants) {
        return "null";
    }

    @Override
    public OptionalBool boolValue() {
        return OptionalBool.of(false);
    }
}

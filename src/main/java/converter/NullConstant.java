package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NullConstant implements LangConstant {
    public static final StdTypeObject TYPE = new StdTypeObject("null");

    @NotNull
    @Override
    public List<Byte> toBytes() {
        throw new UnsupportedOperationException("NullConstant should not be serialized");
    }

    @NotNull
    @Override
    public TypeObject getType() {
        return TYPE;
    }

    @Override
    public @NotNull String name() {
        return "null";
    }
}

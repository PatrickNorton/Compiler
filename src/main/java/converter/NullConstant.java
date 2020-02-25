package main.java.converter;

import java.util.List;

public class NullConstant implements LangConstant {
    public static final StdTypeObject TYPE = new StdTypeObject("null");

    @Override
    public List<Byte> toBytes() {
        throw new UnsupportedOperationException("NullConstant should not be serialized");
    }

    @Override
    public TypeObject getType() {
        return TYPE;
    }
}

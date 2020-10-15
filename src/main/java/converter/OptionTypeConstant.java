package main.java.converter;

import main.java.util.IndexedSet;
import main.java.util.OptionalBool;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class OptionTypeConstant implements LangConstant {
    private final String genericName;
    private final int constantIndex;
    private final TypeObject genericType;

    public OptionTypeConstant(String name, int index, TypeObject type) {
        this.genericName = name;
        this.constantIndex = index;
        this.genericType = type;
    }

    @NotNull
    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(1 + Integer.BYTES);
        bytes.add((byte) ConstantBytes.CLASS.ordinal());
        bytes.addAll(Util.intToBytes(constantIndex));
        return bytes;
    }

    @NotNull
    @Override
    public TypeObject getType() {
        return Builtins.TYPE.generify(TypeObject.optional(genericType));
    }

    @NotNull
    @Override
    public String name(IndexedSet<LangConstant> constants) {
        return genericName + "?";
    }

    @Override
    public OptionalBool boolValue() {
        return OptionalBool.of(true);
    }
}

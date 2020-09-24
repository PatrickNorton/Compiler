package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class OptionConstant implements LangConstant {
    private final TypeObject optionVal;
    private final short constVal;

    public OptionConstant(TypeObject optionVal, short constVal) {
        this.optionVal = optionVal;
        this.constVal = constVal;
    }

    @Override
    @NotNull
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(Short.BYTES + 1);
        bytes.add((byte) ConstantBytes.OPTION.ordinal());
        bytes.addAll(Util.shortToBytes(constVal));
        return bytes;
    }

    @Override
    @NotNull
    public TypeObject getType() {
        return TypeObject.optional(optionVal);
    }

    @Override
    @NotNull
    public String name() {
        return String.format("Option[%d]", constVal);
    }
}

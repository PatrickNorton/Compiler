package main.java.converter;

import main.java.util.IndexedSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OptionConstant implements LangConstant {
    private final TypeObject optionVal;
    private final short constVal;

    public OptionConstant(TypeObject optionVal, short constVal) {
        this.optionVal = optionVal;
        this.constVal = constVal;
    }

    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(Short.BYTES + 1);
        bytes.add((byte) ConstantBytes.OPTION.ordinal());
        bytes.addAll(Util.shortToBytes(constVal));
        return bytes;
    }

    @Override
    public TypeObject getType() {
        return TypeObject.optional(optionVal);
    }

    @Override
    public String name(IndexedSet<LangConstant> constants) {
        return String.format("Option[%s]", constants.get(constVal).name(constants));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptionConstant that = (OptionConstant) o;
        return Objects.equals(optionVal, that.optionVal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(optionVal);
    }
}

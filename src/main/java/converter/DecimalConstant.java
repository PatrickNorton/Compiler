package main.java.converter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DecimalConstant implements LangConstant {
    private final BigDecimal value;

    public DecimalConstant(BigDecimal value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DecimalConstant that = (DecimalConstant) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add((byte) ConstantBytes.DECIMAL.ordinal());
        var bigintVal = value.scaleByPowerOfTen(value.scale()).toBigIntegerExact();
        var byteArray = bigintVal.toByteArray();
        bytes.addAll(Util.intToBytes(byteArray.length));
        bytes.addAll(Util.intToBytes(value.scale()));
        for (byte b : byteArray) {
            bytes.add(b);
        }
        return bytes;
    }

    @Override
    public TypeObject getType() {
        return Builtins.DECIMAL;
    }
}

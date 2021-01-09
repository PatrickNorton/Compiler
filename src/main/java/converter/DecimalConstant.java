package main.java.converter;

import main.java.util.IndexedSet;
import main.java.util.OptionalBool;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    @NotNull
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

    @NotNull
    @Override
    public TypeObject getType() {
        return Builtins.DECIMAL;
    }

    @Override
    public OptionalBool boolValue() {
        return OptionalBool.of(value.signum() != 0);
    }

    @Override
    public Optional<String> strValue() {
        return Optional.of(value.toString());
    }

    @NotNull
    @Override
    public String name(IndexedSet<LangConstant> constants) {
        return value.toString();
    }
}

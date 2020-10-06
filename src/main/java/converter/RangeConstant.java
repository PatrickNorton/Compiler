package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RangeConstant implements LangConstant {
    private final BigInteger start;
    private final BigInteger stop;
    private final BigInteger step;

    public RangeConstant(BigInteger start, BigInteger stop, BigInteger step) {
        this.start = start;
        this.stop = stop;
        this.step = step;
    }

    @Override
    @NotNull
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add((byte) ConstantBytes.BYTES.ordinal());
        addToBytes(bytes, start);
        addToBytes(bytes, stop);
        addToBytes(bytes, step);
        return bytes;
    }

    private static final BigInteger BIG_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger BIG_MIN = BigInteger.valueOf(Integer.MIN_VALUE);

    private static void addToBytes(List<Byte> bytes, BigInteger value) {
        if (value == null) {
            bytes.add((byte) 0);
        } else if (value.compareTo(BIG_MAX) <= 0 && value.compareTo(BIG_MIN) >= 0) {
            bytes.add((byte) 1);
            bytes.addAll(new IntConstant(value.intValue()).toBytes());
        } else {
            bytes.add((byte) 2);
            bytes.addAll(new BigintConstant(value).toBytes());
        }
    }

    @Override
    @NotNull
    public TypeObject getType() {
        return Builtins.RANGE;
    }

    @Override
    public @NotNull String name() {
        var startStr = start == null ? "" : start.toString();
        var stopStr = stop == null ? "" : stop.toString();
        if (step == null) {
            return String.format("[%s:%s]", startStr, stopStr);
        } else {
            return String.format("[%s:%s:%s]", startStr, stopStr, step);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RangeConstant that = (RangeConstant) o;
        return Objects.equals(start, that.start) &&
                Objects.equals(stop, that.stop) &&
                Objects.equals(step, that.step);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, stop, step);
    }
}

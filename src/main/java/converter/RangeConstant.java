package main.java.converter;

import main.java.util.IndexedSet;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RangeConstant implements LangConstant {
    private final BigInteger start;
    private final BigInteger stop;
    private final BigInteger step;

    public RangeConstant(BigInteger start, BigInteger stop, BigInteger step) {
        this.start = start;
        this.stop = stop;
        this.step = step;
    }

    public Optional<BigInteger> getStart() {
        return Optional.ofNullable(start);
    }

    public Optional<BigInteger> getStop() {
        return Optional.ofNullable(stop);
    }

    public Optional<BigInteger> getStep() {
        return Optional.ofNullable(step);
    }

    @Override

    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add((byte) ConstantBytes.RANGE.ordinal());
        addToBytes(bytes, start);
        addToBytes(bytes, stop);
        addToBytes(bytes, step);
        return bytes;
    }

    private static void addToBytes(List<Byte> bytes, BigInteger value) {
        if (value == null) {
            bytes.add((byte) 0);
        } else if (Util.fitsInInt(value)) {
            bytes.add((byte) 1);
            bytes.addAll(Util.intToBytes(value.intValueExact()));
        } else {
            bytes.add((byte) 2);
            bytes.addAll(BigintConstant.convertBigint(value));
        }
    }

    @Override

    public TypeObject getType() {
        return Builtins.RANGE;
    }

    @Override
    public String name(IndexedSet<LangConstant> constants) {
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

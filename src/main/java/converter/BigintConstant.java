package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BigintConstant implements LangConstant {
    private final BigInteger value;

    public BigintConstant(BigInteger value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BigintConstant that = (BigintConstant) o;
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
        var biBytes = bigintBytes();
        bytes.add((byte) ConstantBytes.BIGINT.ordinal());
        bytes.addAll(Util.intToBytes(biBytes.size() / Integer.BYTES));
        bytes.addAll(bigintBytes());
        return bytes;
    }

    @NotNull
    @Override
    public TypeObject getType() {
        return Builtins.INT;
    }

    @NotNull
    private List<Byte> bigintBytes() {
        var byteArray = value.toByteArray();
        var addedBytes = Integer.BYTES - byteArray.length % Integer.BYTES;
        List<Byte> bytes = new ArrayList<>(byteArray.length);
        for (int i = 0; i < addedBytes; i++) {
            bytes.add((byte) 0);
        }
        for (byte b : byteArray) {
            bytes.add(b);
        }
        return bytes;
    }

    @NotNull
    @Override
    public String name() {
        return value.toString();
    }
}

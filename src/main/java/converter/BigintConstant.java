package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BigintConstant implements LangConstant {
    private BigInteger value;

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

    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>();
        var biBytes = bigintBytes();
        bytes.add((byte) ConstantBytes.BIGINT.ordinal());
        bytes.addAll(Util.intToBytes(biBytes.size()));
        bytes.addAll(bigintBytes());
        return bytes;
    }

    @NotNull
    private List<Byte> bigintBytes() {
        var byteArray = value.toByteArray();
        List<Byte> bytes = new ArrayList<>(byteArray.length);
        for (byte b : byteArray) {
            bytes.add(b);
        }
        return bytes;
    }
}

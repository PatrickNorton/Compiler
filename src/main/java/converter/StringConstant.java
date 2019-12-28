package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class StringConstant implements LangConstant {
    private final String value;

    public StringConstant(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StringConstant that = (StringConstant) o;
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
        bytes.add((byte) ConstantBytes.STR.ordinal());
        bytes.addAll(Util.intToBytes(value.length()));
        bytes.addAll(strBytes());
        return bytes;
    }

    @NotNull
    private List<Byte> strBytes() {
        List<Byte> bytes = new ArrayList<>();
        var byteBuffer = StandardCharsets.UTF_8.encode(value);
        for (byte b : byteBuffer.array()) {
            bytes.add(b);
        }
        return bytes;
    }
}

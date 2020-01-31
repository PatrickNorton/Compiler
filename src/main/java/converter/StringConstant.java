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
        var strBytes = strBytes();
        bytes.add((byte) ConstantBytes.STR.ordinal());
        bytes.addAll(Util.intToBytes(strBytes.size()));
        bytes.addAll(strBytes);
        return bytes;
    }

    @Override
    public TypeObject getType() {
        return Builtins.STR;
    }

    @NotNull
    private List<Byte> strBytes() {
        var byteArray = value.getBytes(StandardCharsets.UTF_8);
        List<Byte> bytes = new ArrayList<>(byteArray.length);
        for (byte b : byteArray) {
            bytes.add(b);
        }
        return bytes;
    }
}

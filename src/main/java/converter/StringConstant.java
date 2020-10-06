package main.java.converter;

import main.java.util.OptionalBool;
import org.jetbrains.annotations.Contract;
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

    public String getValue() {
        return value;
    }

    @NotNull
    @Override
    public List<Byte> toBytes() {
        List<Byte> bytes = new ArrayList<>(value.length() + Integer.BYTES + 1);  // Guess capacity
        bytes.add((byte) ConstantBytes.STR.ordinal());
        bytes.addAll(strBytes(value));
        return bytes;
    }

    @NotNull
    @Override
    public TypeObject getType() {
        return Builtins.STR;
    }

    @NotNull
    public static List<Byte> strBytes(@NotNull String value) {
        var byteArray = value.getBytes(StandardCharsets.UTF_8);
        List<Byte> bytes = new ArrayList<>(byteArray.length + Integer.BYTES);
        bytes.addAll(Util.intToBytes(byteArray.length));
        for (byte b : byteArray) {
            bytes.add(b);
        }
        return bytes;
    }

    @NotNull
    public static byte[] strByteArray(@NotNull String value) {
        var byteArray = value.getBytes(StandardCharsets.UTF_8);
        var result = new byte[byteArray.length + Integer.BYTES];
        System.arraycopy(byteArray, 0, result, Integer.BYTES, byteArray.length);
        System.arraycopy(Util.toByteArray(byteArray.length), 0, result, 0, Integer.BYTES);
        return result;
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String name() {
        return '"' + value + '"';
    }

    @Override
    public OptionalBool boolValue() {
        return OptionalBool.of(!value.isEmpty());
    }
}

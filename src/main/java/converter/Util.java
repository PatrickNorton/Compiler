package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Util {
    public static final byte[] MAGIC_NUMBER = toByteArray(0x0ABADE66);  // A bad egg :)
    public static final String FILE_EXTENSION = ".newlang";
    public static final String EXPORTS_FILENAME = "__exports__" + FILE_EXTENSION;

    private Util() {}

    @NotNull
    @Contract(pure = true)
    static List<Byte> intToBytes(int value) {
        return List.of(
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        );
    }

    private static final List<Byte> ZERO_INT_BYTES = List.of((byte) 0, (byte) 0, (byte) 0, (byte) 0);

    static List<Byte> zeroToBytes() {
        return ZERO_INT_BYTES;
    }

    @NotNull
    @Contract(pure = true)
    static List<Byte> shortToBytes(short value) {
        return List.of(
                (byte) (value >>> 8),
                (byte) value
        );
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    static byte[] toByteArray(int value) {
        return new byte[] {
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        };
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    static byte[] toByteArray(short value) {
        return new byte[] {
                (byte) (value >>> 8),
                (byte) value
        };
    }

    @NotNull
    static byte[] toByteArray(@NotNull List<Byte> values) {
        var bytes = new byte[values.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = values.get(i);
        }
        return bytes;
    }

    @NotNull
    @Contract(pure = true)
    static byte[] unBox(@NotNull Byte[] bytes) {
        byte[] byteArray = new byte[bytes.length];
        int index = 0;
        for (byte b : bytes) {
            byteArray[index++] = b;
        }
        return byteArray;
    }

    static void emplace(List<Byte> original, @NotNull List<Byte> toInsert, int start) {
        for (int i = 0; i < toInsert.size(); i++) {
            original.set(start + i, toInsert.get(i));
        }
    }

    @NotNull
    static List<Byte> strBytes(@NotNull String value) {
        var byteArray = value.getBytes(StandardCharsets.UTF_8);
        List<Byte> bytes = new ArrayList<>(byteArray.length);
        for (byte b : byteArray) {
            bytes.add(b);
        }
        return bytes;
    }
}

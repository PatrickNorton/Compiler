package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
}

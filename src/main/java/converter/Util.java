package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Util {
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
}

package main.java.converter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Static class for int & byte-related functions.
 *
 * @author Patrick Norton
 */
public final class Util {
    public static final byte[] MAGIC_NUMBER = toByteArray(0x0ABADE66);  // A bad egg :)
    public static final String FILE_EXTENSION = ".newlang";
    public static final String EXPORTS_FILENAME = "__exports__" + FILE_EXTENSION;
    public static final String BYTECODE_EXTENSION = ".nbyte";

    private Util() {}

    /**
     * Converts an {@code int} into a list of {@code bytes}.
     * <p>
     *     The returned list will always have a length equal to {@link
     *     Integer#BYTES} (should always be 4). If {@code value} is known to be
     *     0, use {@link #zeroToBytes()} instead.
     * </p>
     *
     * @param value The int value to get the bytes of
     * @return The list of bytes
     */
    @NotNull
    @Contract(pure = true)
    @Unmodifiable
    public static List<Byte> intToBytes(int value) {
        return List.of(
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value
        );
    }

    private static final List<Byte> ZERO_INT_BYTES = List.of((byte) 0, (byte) 0, (byte) 0, (byte) 0);

    /**
     * Optimized version of {@link #intToBytes(int)} for when the value is known
     * to be 0.
     *
     * @return {@link #intToBytes}{@code (0)}
     */
    static List<Byte> zeroToBytes() {
        return ZERO_INT_BYTES;
    }

    private static final List<Byte> ZERO_SHORT_BYTES = List.of((byte) 0, (byte) 0);

    /**
     * Optimized version of {@link #shortToBytes(short)} for when the value is known
     * to be 0.
     *
     * @return {@link #shortToBytes}{@code (0)}
     */
    static List<Byte> shortZeroBytes() {
        return ZERO_SHORT_BYTES;
    }

    /**
     * Converts a {@code short} into a list of {@code bytes}.
     * <p>
     *     The returned list will always have a length equal to {@link
     *     Short#BYTES} (should always be 2). If {@code value} is known to be
     *     0, use {@link #shortZeroBytes()} instead.
     * </p>
     *
     * @param value The int value to get the bytes of
     * @return The list of bytes
     */
    @NotNull
    @Contract(pure = true)
    @Unmodifiable
    public static List<Byte> shortToBytes(short value) {
        return List.of(
                (byte) (value >>> 8),
                (byte) value
        );
    }

    /**
     * Equivalent to {@link #intToBytes(int)}, but returns a {@code byte[]}
     * instead of a {@code List<Byte>}.
     * <p>
     *     Because Java doesn't have good unboxing facilities, this prevents
     *     having to iterate over a list too many times.
     * </p>
     *
     * @param value The int value to get the bytes of
     * @return The list of bytes
     */
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

    private static final byte[] ZERO_BYTE_ARRAY = new byte[] {(byte) 0, (byte) 0, (byte) 0, (byte) 0};

    /**
     * Optimized version of {@link #toByteArray(int)} for when the value is known
     * to be 0.
     *
     * @return {@link #toByteArray}{@code (0)}
     */
    static byte[] zeroByteArray() {
        return ZERO_BYTE_ARRAY;
    }

    /**
     * Equivalent to {@link #shortToBytes(short)}, but returns a {@code byte[]}
     * instead of a {@code List<Byte>}.
     * <p>
     *     Because Java doesn't have good unboxing facilities, this prevents
     *     having to iterate over a list too many times.
     * </p>
     *
     * @param value The short value to get the bytes of
     * @return The list of bytes
     */
    @NotNull
    @Contract(value = "_ -> new", pure = true)
    static byte[] toByteArray(short value) {
        return new byte[] {
                (byte) (value >>> 8),
                (byte) value
        };
    }

    /**
     * Unboxes a {@code List<Byte>} into a {@code byte[]}.
     *
     * @param values The list of bytes to unbox
     * @return The unboxed list
     */
    @NotNull
    static byte[] toByteArray(@NotNull List<Byte> values) {
        var bytes = new byte[values.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = values.get(i);
        }
        return bytes;
    }

    static List<Byte> toByteList(@NotNull byte[] values) {
        List<Byte> bytes = new ArrayList<>(values.length);
        for (var value : values) {
            bytes.add(value);
        }
        return bytes;
    }

    /**
     * Overwrites a list with another list, starting at the given index.
     * <code><pre>
     * var original = [0, 1, 2, 3, 4, 5];  // Not correct syntax, but whatever
     * Util.emplace(original, [9, 8, 7, 6], 1);
     * System.out.println(original);  // [0, 9, 8, 7, 6, 5]
     * </pre></code>
     *
     * @param original The list to be modified
     * @param toInsert The list to insert into the other
     * @param start The index to start at
     */
    static void emplace(List<Byte> original, @NotNull List<Byte> toInsert, int start) {
        for (int i = 0; i < toInsert.size(); i++) {
            original.set(start + i, toInsert.get(i));
        }
    }

    private static final BigInteger BIG_MAX = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger BIG_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
    private static final BigInteger BYTE_MAX = BigInteger.valueOf(Byte.MAX_VALUE);
    private static final BigInteger BYTE_MIN = BigInteger.valueOf(Byte.MIN_VALUE);

    /**
     * Determines if the value given can fit into an {@code int} or not.
     * <p>
     *     If this returns {@code true}, then {@code value.}{@link
     *     BigInteger#intValueExact() intValueExact()} will not throw an
     *     exception. If this returns {@code false}, it will throw.
     * </p>
     *
     * @param value The value to check
     * @return If the value will fit into an int
     */
    static boolean fitsInInt(BigInteger value) {
        return value.compareTo(BIG_MAX) <= 0 && value.compareTo(BIG_MIN) >= 0;
    }

    /**
     * Determines if the value given can fit into a {@code byte} or not.
     * <p>
     *     If this returns {@code true}, then {@code value.}{@link
     *     BigInteger#byteValueExact()} byteValueExact()} will not throw an
     *     exception. If this returns {@code false}, it will throw.
     * </p>
     *
     * @param value The value to check
     * @return If the value will fit into a byte
     */
    static boolean fitsInByte(BigInteger value) {
        return value.compareTo(BYTE_MAX) <= 0 && value.compareTo(BYTE_MIN) >= 0;
    }

    static int decimalDigits(int value) {
        if (value == 0) {
            return 1;
        }
        return (int) (Math.log10(Math.abs(value)) + 1);
    }
}

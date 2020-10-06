package main.java.util;

import java.util.NoSuchElementException;

/**
 * An optimized version of {@link java.util.Optional} for booleans.
 * @implNote This class can and should be expanded to have equivalent methods
 *           corresponding to each of those in {@link java.util.Optional}, but
 *           since this takes time (and would wind up with a lot of 'unused
 *           method' errors), it hasn't been done up front.
 * @author Patrick Norton
 * @see java.util.Optional
 */
public final class OptionalBool {
    private byte value;

    private static final OptionalBool EMPTY = new OptionalBool();
    private static final OptionalBool TRUE = new OptionalBool(true);
    private static final OptionalBool FALSE = new OptionalBool(false);

    private OptionalBool() {
        value = -1;
    }

    private OptionalBool(boolean value) {
        this.value = (byte) (value ? 1 : 0);
    }

    public static OptionalBool of(boolean value) {
        return value ? TRUE : FALSE;
    }

    public static OptionalBool empty() {
        return EMPTY;
    }

    /**
     * If a value is present, returns {@code true}, otherwise {@code false}.
     *
     * @return {@code true} if a value is present, otherwise {@code false}
     * @see java.util.Optional#isPresent()
     */
    public boolean isPresent() {
        return value >= 0;
    }

    /**
     * If a value is not present, returns {@code true}, otherwise
     * {@code false}.
     *
     * @return {@code true} if a value is not present, otherwise {@code false}
     * @see java.util.Optional#isEmpty()
     */
    public boolean isEmpty() {
        return value < 0;
    }

    /**
     * If a value is present, returns the value, otherwise throws
     * {@code NoSuchElementException}.
     *
     * @return the non-{@code null} value described by this {@code OptionalBool}
     * @throws NoSuchElementException if no value is present
     * @see java.util.Optional#orElseThrow()
     */
    public boolean orElseThrow() {
        if (value < 0) {
            throw new NoSuchElementException("Called 'orElseThrow' on an empty OptionalUint");
        }
        return value != 0;
    }
}

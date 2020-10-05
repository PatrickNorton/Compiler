package main.java.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * An optimized version of {@link java.util.Optional} for unsigned integers.
 * @implNote This class can and should be expanded to have equivalent methods
 *           corresponding to each of those in {@link java.util.Optional}, but
 *           since this takes time (and would wind up with a lot of 'unused
 *           method' errors), it hasn't been done up front.
 * @author Patrick Norton
 * @see java.util.Optional
 */
public final class OptionalUint {
    private final int value;

    private static final OptionalUint EMPTY = new OptionalUint();

    private OptionalUint() {
        this.value = -1;
    }

    private OptionalUint(int value) {
        if (value < 0) {
            throw new IllegalArgumentException(String.format("%d is less than 0", value));
        }
        this.value = value;
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
     * If a value is  not present, returns {@code true}, otherwise
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
     * @return the non-{@code null} value described by this {@code Optional}
     * @throws NoSuchElementException if no value is present
     * @see java.util.Optional#orElseThrow()
     */
    public int orElseThrow() {
        if (value < 0) {
            throw new NoSuchElementException("Called 'orElseThrow' on an empty OptionalUint");
        }
        return value;
    }

    public <X extends Throwable> int orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (value >= 0) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Returns an {@code OptionalUint} describing the given non-negative
     * value.
     *
     * @param value the value to describe, which must be non-negative
     * @return an {@code OptionalUint} with the value present
     * @throws IllegalArgumentException if value is negative
     * @see java.util.Optional#of(Object)
     */
    @Contract("_ -> new")
    @NotNull
    public static OptionalUint of(int value) {
        return new OptionalUint(value);
    }

    /**
     * Returns an empty {@code OptionalUint} instance.  No value is present for
     * this {@code OptionalUint}.
     *
     * @apiNote
     * Though it may be tempting to do so, avoid testing if an object is empty
     * by comparing with {@code ==} against instances returned by
     * {@code OptionalUint.empty()}.  There is no guarantee that it is a
     * singleton. Instead, use {@link #isPresent()}.
     *
     * @return an empty {@code OptionalUint}
     * @see java.util.Optional#empty()
     */
    public static OptionalUint empty() {
        return EMPTY;
    }

    /**
     * Returns a {@code OptionalUint} representing the given value if it is
     * non-negative, or an empty value otherwise.
     *
     * @apiNote
     * The closest analogue in {@link java.util.Optional Optional} is the
     * {@link java.util.Optional#ofNullable(Object) ofNullable} method, however
     * this is differently named b/c that name may be used in future for
     * conversions from {@link Integer}, which would be closer in meaning.
     *
     * @param value The value to convert, which may be negative
     * @return an {@code OptionalUInt} possibly containing the value
     */
    public static OptionalUint ofSigned(int value) {
        return value < 0 ? empty() : of(value);
    }
}

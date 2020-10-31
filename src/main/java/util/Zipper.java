package main.java.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Provides an iterator for multiple items in parallel, using {@link Pair} to
 * join the values.
 *
 * @param <A> The type of the first iterated item
 * @param <B> The type of the second iterated item
 * @author Patrick Norton
 */
public final class Zipper<A, B> implements Iterator<Pair<A, B>> {
    private final Iterator<A> first;
    private final Iterator<B> second;

    private Zipper(Iterator<A> first, Iterator<B> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean hasNext() {
        return first.hasNext() && second.hasNext();
    }

    @Override
    public Pair<A, B> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return Pair.of(first.next(), second.next());
    }

    /**
     * Constructs a {@link Zipper} out of the two given items.
     *
     * @param first The first iterable
     * @param second The second iterable
     * @param <A> The type iterated over by {@code first}
     * @param <B> The type iterated over by {@code second}
     * @return The iterable item
     */

    public static <A, B> Iterable<Pair<A, B>> of(Iterable<A> first,Iterable<B> second) {
        return () -> new Zipper<>(first.iterator(), second.iterator());
    }

    /**
     * Constructs a {@link Zipper} out of the two given items.
     *
     * @param first The first iterable
     * @param second The second iterable
     * @param <A> The type iterated over by {@code first}
     * @param <B> The type iterated over by {@code second}
     * @return The iterable item
     */

    public static <A, B> Iterable<Pair<A, B>> of(A[] first,B[] second) {
        return () -> new Zipper<>(Arrays.asList(first).iterator(), Arrays.asList(second).iterator());
    }

    /**
     * Constructs a {@link Zipper} out of the two given items.
     *
     * @param first The first iterable
     * @param second The second iterable
     * @param <A> The type iterated over by {@code first}
     * @param <B> The type iterated over by {@code second}
     * @return The iterable item
     */

    public static <A, B> Iterable<Pair<A, B>> of(A[] first,Iterable<B> second) {
        return () -> new Zipper<>(Arrays.asList(first).iterator(), second.iterator());
    }
}

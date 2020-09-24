package main.java.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Zipper<A, B> implements Iterator<Pair<A, B>> {
    private final Iterator<A> first;
    private final Iterator<B> second;

    private Zipper(@NotNull Iterator<A> first, @NotNull Iterator<B> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean hasNext() {
        return first.hasNext() && second.hasNext();
    }

    @Override
    @NotNull
    public Pair<A, B> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return Pair.of(first.next(), second.next());
    }

    @Contract(value = "_, _ -> new", pure = true)
    @NotNull
    public static <A, B> Iterable<Pair<A, B>> of(@NotNull Iterable<A> first, @NotNull Iterable<B> second) {
        return () -> new Zipper<>(first.iterator(), second.iterator());
    }
}

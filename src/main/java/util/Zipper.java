package main.java.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Zipper<A, B> implements Iterable<Pair<A, B>> {
    private final Iterable<A> first;
    private final Iterable<B> second;

    public Zipper(@NotNull Iterable<A> first, @NotNull Iterable<B> second) {
        this.first = first;
        this.second = second;
    }

    @NotNull
    @Override
    public Iterator<Pair<A, B>> iterator() {
        return new Zipperator<>(first.iterator(), second.iterator());
    }

    private static final class Zipperator<A, B> implements Iterator<Pair<A, B>> {
        private final Iterator<A> first;
        private final Iterator<B> second;

        public Zipperator(Iterator<A> first, Iterator<B> second) {
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
    }
}

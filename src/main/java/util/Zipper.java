package main.java.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Zipper<A, B, C extends Iterable<A>, D extends Iterable<B>>
        implements Iterable<Pair<A, B>>, Iterator<Pair<A, B>> {
    private final Iterator<A> first;
    private final Iterator<B> second;

    public Zipper(@NotNull C first, @NotNull D second) {
        this.first = first.iterator();
        this.second = second.iterator();
    }


    @NotNull
    @Override
    public Iterator<Pair<A, B>> iterator() {
        return this;
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

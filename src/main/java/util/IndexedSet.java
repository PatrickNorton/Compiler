package main.java.util;

import java.util.Set;

public interface IndexedSet<E> extends Set<E> {
    E get(int index);
    void set(int index, E value);
    E remove(int index);
    int indexOf(E object);

    static <T> IndexedSet<T> unmodifiable(IndexedSet<T> value) {
        return new UnmodifiableIndexedSet<>(value);
    }
}

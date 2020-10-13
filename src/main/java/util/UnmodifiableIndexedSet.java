package main.java.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

final class UnmodifiableIndexedSet<T> implements IndexedSet<T> {
    private final IndexedSet<T> value;

    public UnmodifiableIndexedSet(IndexedSet<T> value) {
        this.value = value;
    }

    @Override
    public T get(int index) {
        return value.get(index);
    }

    @Override
    public void set(int index, T value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(T object) {
        return value.indexOf(object);
    }

    @Override
    public int size() {
        return value.size();
    }

    @Override
    public boolean isEmpty() {
        return value.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return value.contains(o);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new UnmodifiableIterator<>(value.iterator());
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return value.toArray();
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] a) {
        return value.toArray(a);
    }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return value.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    private static final class UnmodifiableIterator<T> implements Iterator<T> {
        private final Iterator<T> value;

        public UnmodifiableIterator(Iterator<T> value) {
            this.value = value;
        }

        @Override
        public boolean hasNext() {
            return value.hasNext();
        }

        @Override
        public T next() {
            return value.next();
        }
    }
}
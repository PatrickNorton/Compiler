package util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * An implementation of a circular buffer. Allows amortized constant-time
 * access and end removal of items. The only reason to use this over {@link
 * java.util.ArrayDeque} is the access of elements by index.
 *
 * @param <T> The type of the list element
 *
 * @author Patrick Norton
 * @see Deque
 * @see List
 */
public class CircularBuffer<T> extends AbstractCollection<T> implements Deque<T>, List<T>, RandomAccess {
    private static final int DEFAULT_SIZE = 16;

    private Object[] values;
    private int size;
    private int start;

    @Contract(pure = true)
    public CircularBuffer() {
        values = new Object[DEFAULT_SIZE];
        size = 0;
        start = 0;
    }

    @Override
    public void addFirst(T t) {
        ensureSize(size + 1);
        values[internalIndex(-1)] = t;
        decrementStart();
        size++;
    }

    @Override
    public void addLast(T t) {
        ensureSize(++size);
        values[internalIndex(size - 1)] = t;
    }

    @Override
    public boolean offerFirst(T t) {
        addFirst(t);
        return true;
    }

    @Override
    public boolean offerLast(T t) {
        addLast(t);
        return true;
    }

    @Override
    public T removeFirst() {
        T item = peekFirst();
        values[internalIndex(0)] = null;
        incrementStart();
        size--;
        return item;
    }

    @Override
    public T removeLast() {
        T item = peekLast();
        values[internalIndex(size - 1)] = null;
        size--;
        return item;
    }

    @Override
    public T pollFirst() {
        if (size == 0) return null;
        T value = peekFirst();
        removeFirst();
        return value;
    }

    @Override
    public T pollLast() {
        if (size == 0) return null;
        T value = peekLast();
        removeLast();
        return value;
    }

    @Override
    public T getFirst() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return peekFirst();
    }

    @Override
    public T getLast() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return peekLast();
    }

    @Contract(pure = true)
    @Override
    @SuppressWarnings("unchecked")
    public T peekFirst() {
        return (T) values[internalIndex(0)];
    }

    @Contract(pure = true)
    @Override
    @SuppressWarnings("unchecked")
    public T peekLast() {
        return (T) values[internalIndex(size - 1)];
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        int index = indexOf(o);
        if (index == -1) {
            return false;
        } else {
            remove(index);
            return true;
        }
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        int index = lastIndexOf(o);
        if (index == -1) {
            return false;
        } else {
            remove(index);
            return true;
        }
    }

    @Override
    public boolean offer(T t) {
        return offerFirst(t);
    }

    @Override
    public T remove() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return pollFirst();
    }

    @Override
    public T poll() {
        return pollFirst();
    }

    @Override
    public T element() {
        return getFirst();
    }

    @Contract(pure = true)
    @Override
    public T peek() {
        return peekFirst();
    }

    @Override
    public void push(T t) {
        addFirst(t);
    }

    @Override
    public T pop() {
        return removeFirst();
    }

    @Contract(pure = true)
    @Override
    public boolean contains(Object o) {
        for (T i : this) {
            if (o.equals(i)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int current = 0;

            @Override
            public boolean hasNext() {
                return current < size();
            }

            @Override
            public T next() {
                return get(current++);
            }
        };
    }

    @Override
    public boolean add(T t) {
        addLast(t);
        return true;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        ensureSize(size + c.size());
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends T> c) {
        ensureSize(size + c.size());
        for (T value : c) {
            add(index++, value);
        }
        return true;
    }

    @Override
    public void clear() {
        values = new Object[DEFAULT_SIZE];
        size = 0;
        start = 0;
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CircularBuffer)) return false;
        return Arrays.equals(values, ((CircularBuffer<?>) o).values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Contract(pure = true)
    @Override
    @SuppressWarnings("unchecked")
    public T get(int index) {
        verifyIndex(index);
        return (T) values[internalIndex(index)];
    }

    @Override
    public T set(int index, T element) {
        T oldValue = get(index);
        values[internalIndex(index)] = element;
        return oldValue;
    }

    @Override
    public void add(int index, T element) {
        verifyIndex(index);
        ensureSize(size + 1);
        if (index == size) {
            addLast(element);
            return;
        } else if (index == 0) {
            addFirst(element);
            return;
        }
        int internalIndex = internalIndex(index);
        if (internalIndex < start) {
            System.arraycopy(values, internalIndex, values, internalIndex + 1, size - index);
        } else if (start + size + 1 < values.length) {
            System.arraycopy(values, index, values, index + 1, size - index);
        } else {
            // Copy the wrapped values
            System.arraycopy(values, 0, values, 1, start + size - values.length);
            values[0] = values[values.length - 1];
            System.arraycopy(values, internalIndex, values, internalIndex + 1, values.length - internalIndex - 1);
        }
        values[internalIndex] = element;
        size++;
    }

    @Override
    public T remove(int index) {
        verifyIndex(index);
        if (index == 0) {
            return removeFirst();
        } else if (index == size) {
            return removeLast();
        }
        T old = get(index);
        int internalIndex = internalIndex(index);
        if (internalIndex < start) {
            System.arraycopy(values, internalIndex + 1, values, internalIndex, size - index);
        } else if (start + size < values.length) {
            System.arraycopy(values, index + 1, values, index, size - index);
        } else {
            System.arraycopy(values, internalIndex + 1, values, internalIndex, values.length - internalIndex - 1);
            values[values.length - 1] = values[0];
            System.arraycopy(values, 1, values, 0, start + size - values.length);
        }
        values[internalIndex(--size)] = null;
        return old;
    }

    @Override
    public int indexOf(Object o) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(o, get(i))) {
                return i;
            }
            i++;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        for (int i = size; i > 0; --i) {
            if (Objects.equals(o, get(i))) {
                return i;
            }
        }
        return -1;
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator() {
        return new InternalIterator(0);
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator(int index) {
        return new InternalIterator(index);
    }

    private class InternalIterator implements ListIterator<T> {
        private int current;

        @Contract(pure = true)
        InternalIterator(int start) {
            current = start;
        }

        @Override
        public boolean hasNext() {
            return current < size;
        }

        @Override
        public T next() {
            return get(current++);
        }

        @Override
        public boolean hasPrevious() {
            return current > 0;
        }

        @Override
        public T previous() {
            return get(--current);
        }

        @Override
        public int nextIndex() {
            return current;
        }

        @Override
        public int previousIndex() {
            return current - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(T t) {
            CircularBuffer.this.add(current++, t);
        }
    }

    @NotNull
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return this.size;
    }

    @NotNull
    @Override
    public Iterator<T> descendingIterator() {
        return new Iterator<>() {
            private int index = size;

            @Override
            public boolean hasNext() {
                return index > 0;
            }

            @Override
            public T next() {
                return get(--index);
            }
        };
    }

    /**
     * Ensure the internal value buffer is the size required.
     *
     * @param newSize The size to ensure
     */
    private void ensureSize(int newSize) {
        if (values.length > newSize) {
            return;
        }
        if (start + size > values.length) {
            int newLength = values.length > Integer.MAX_VALUE / 2 ? newSize : values.length * 2;
            int firstHalfLength = values.length - start;
            Object[] newArray = new Object[newLength];
            System.arraycopy(values, start, newArray, 0, firstHalfLength);
            System.arraycopy(values, 0, newArray, firstHalfLength, size - firstHalfLength);
            values = newArray;
            start = 0;
        } else {
            values = Arrays.copyOf(values, values.length << 1);
        }
    }

    private void verifyIndex(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException();
    }

    @Contract(pure = true)
    private int internalIndex(int index) {
        return Math.floorMod(start + index, values.length);
    }

    private void incrementStart() {
        start = Math.floorMod(start + 1, values.length);
    }

    private void decrementStart() {
        start = Math.floorMod(start - 1, values.length);
    }
}

package main.java.util;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public final class IntAllocator extends AbstractCollection<Integer> implements Collection<Integer> {
    private int max;
    private final SortedSet<Integer> removed;

    public IntAllocator() {
        max = 0;
        removed = new TreeSet<>();
    }

    public int getNext() {
        if (removed.isEmpty()) {
            return max++;
        } else {
            int n = removed.first();
            removed.remove(n);
            return n;
        }
    }

    public void remove(int num) {
        if (contains(num)) {
            if (num != max - 1) {
                removed.add(num);
            } else {
                max--;
            }
        }
    }

    public boolean contains(int value) {
        return value < max && !removed.contains(value);
    }

    @Override
    public int size() {
        return max - removed.size();
    }

    @NotNull
    @Override
    public Iterator<Integer> iterator() {
        return new IntIterator();
    }

    @Override
    public boolean add(Integer integer) {
        if (max == integer) {
            max++;
            return true;
        } else if (removed.contains(integer)) {
            removed.remove(integer);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Integer)) return false;
        remove(((Integer) o).intValue());
        return true;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {
        max = 0;
        removed.clear();
    }

    @Override
    public boolean isEmpty() {
        return max == 0;
    }

    @Override
    public boolean contains(Object o) {
        return o instanceof Integer && contains(((Integer) o).intValue());
    }

    private class IntIterator implements Iterator<Integer> {
        private int current = 0;

        @Override
        public boolean hasNext() {
            return current < max;
        }

        @Override
        public Integer next() {
            while (removed.contains(current)) {
                current++;
            }
            return current++;
        }
    }
}
